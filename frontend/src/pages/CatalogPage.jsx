import { useState, useEffect, useCallback, useRef } from 'react';
import { getCategories, getProducts, getOperations, search } from '../api/api';
import ProductCard from '../components/ProductCard';
import Pagination from '../components/Pagination';
import { SkeletonGrid, ErrorState } from '../components/LoadingState';
import { useToast } from '../components/ToastContext';

const PER_PAGE = 48;

const FILTER_FIELDS = [
  { key: 'toolMaterial', label: 'Tool Material' },
  { key: 'workpieceMaterial', label: 'Workpiece Material' },
  { key: 'machineType', label: 'Machine Type' },
  { key: 'machineBrand', label: 'Machine Brand' },
  { key: 'cuttingType', label: 'Cutting Type' },
  { key: 'shankMm', label: 'Shank (mm)' },
];

function CategoryTree({ categories, selected, onSelect }) {
  const [expanded, setExpanded] = useState({});

  function toggle(id) {
    setExpanded(prev => ({ ...prev, [id]: !prev[id] }));
  }

  function renderNode(node, depth = 0) {
    const hasChildren = node.children && node.children.length > 0;
    const isExpanded = expanded[node.id];
    const isSelected = selected && selected.id === node.id;
    const label = node.name || node.slug || node.groupCode || node.label || 'Unnamed';

    return (
      <div key={node.id} className="category-tree-item">
        <div className={`category-tree-row${isSelected ? ' active' : ''}`}>
          <button
            className="category-tree-select"
            onClick={() => onSelect(isSelected ? null : { type: node.type, id: node.id })}
          >
            <span className="category-tree-text">{label}</span>
          </button>
          {hasChildren ? (
            <button
              className={`category-chevron-btn${isExpanded ? ' open' : ''}`}
              onClick={() => toggle(node.id)}
              aria-label={isExpanded ? 'Collapse' : 'Expand'}
            >
              ▶
            </button>
          ) : (
            <span style={{ width: 36, flexShrink: 0 }} />
          )}
        </div>
        {hasChildren && isExpanded && (
          <div className="category-children">
            {node.children.map(child => renderNode(child, depth + 1))}
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="category-tree">
      <div className="category-tree-header">Catalog</div>
      <div className="category-tree-list">
        <div className="category-tree-item">
          <div className={`category-tree-row${!selected ? ' active' : ''}`}>
            <button className="category-tree-select category-tree-select--all" onClick={() => onSelect(null)}>
              <span className="category-tree-text">All Products</span>
            </button>
            <span style={{ width: 36, flexShrink: 0 }} />
          </div>
        </div>
        {categories.map(cat => renderNode(cat))}
      </div>
    </div>
  );
}

function FiltersPanel({ filters, onChange, onClear }) {
  function handleChange(key, value) {
    onChange({ ...filters, [key]: value });
  }

  const hasActive = Object.values(filters).some(v => v !== '');

  return (
    <div className="filters-panel">
      <div className="filters-header">
        <span>Filters</span>
        {hasActive && (
          <button className="filters-clear-btn" onClick={onClear}>Clear all</button>
        )}
      </div>
      <div className="filters-body">
        {FILTER_FIELDS.map(f => (
          <div key={f.key} className="form-group">
            <label className="form-label">{f.label}</label>
            <input
              className="form-control"
              type="text"
              placeholder={`Filter by ${f.label.toLowerCase()}…`}
              value={filters[f.key] || ''}
              onChange={e => handleChange(f.key, e.target.value)}
            />
          </div>
        ))}

        <div className="form-group">
          <label className="form-label">Diameter (mm)</label>
          <div className="filter-range">
            <input
              className="form-control"
              type="number"
              placeholder="Min"
              value={filters.dMmMin || ''}
              onChange={e => handleChange('dMmMin', e.target.value)}
            />
            <input
              className="form-control"
              type="number"
              placeholder="Max"
              value={filters.dMmMax || ''}
              onChange={e => handleChange('dMmMax', e.target.value)}
            />
          </div>
        </div>

        <div className="form-group">
          <label className="form-label">Availability</label>
          <select
            className="form-control"
            value={filters.inStock || ''}
            onChange={e => handleChange('inStock', e.target.value)}
          >
            <option value="">All</option>
            <option value="true">In Stock</option>
            <option value="false">Out of Stock</option>
          </select>
        </div>

        <div className="form-group">
          <label className="form-label">Product Type</label>
          <input
            className="form-control"
            type="text"
            placeholder="Product type…"
            value={filters.productType || ''}
            onChange={e => handleChange('productType', e.target.value)}
          />
        </div>

        <div className="form-group">
          <label className="form-label">Ball Bearing</label>
          <select
            className="form-control"
            value={filters.hasBallBearing || ''}
            onChange={e => handleChange('hasBallBearing', e.target.value)}
          >
            <option value="">Any</option>
            <option value="true">Yes</option>
            <option value="false">No</option>
          </select>
        </div>
      </div>
    </div>
  );
}

const EMPTY_FILTERS = {
  toolMaterial: '', workpieceMaterial: '', machineType: '', machineBrand: '',
  cuttingType: '', dMmMin: '', dMmMax: '', shankMm: '', hasBallBearing: '',
  productType: '', inStock: '',
};

function normalizeTree(sections) {
  return sections.map(s => ({
    ...s,
    type: 'section',
    children: (s.categories || []).map(c => ({
      ...c,
      type: 'category',
      children: (c.groups || []).map(g => ({
        ...g,
        type: 'group',
        children: []
      }))
    }))
  }));
}


export default function CatalogPage({ locale }) {
  const toast = useToast();
  const [categories, setCategories] = useState([]);
  const [selectedNode, setSelectedNode] = useState(null);
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [products, setProducts] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [catLoading, setCatLoading] = useState(true);
  const [operations, setOperations] = useState([]);
  const [selectedOperation, setSelectedOperation] = useState(null);

  const [mobileFiltersOpen, setMobileFiltersOpen] = useState(false);

  // Search state
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState(null);
  const [searchTotal, setSearchTotal] = useState(0);
  const [searchPage, setSearchPage] = useState(1);
  const [searchLoading, setSearchLoading] = useState(false);
  const inputRef = useRef(null);

  useEffect(() => {
    setCatLoading(true);
    getCategories(locale)
      .then(data => {
        const raw = Array.isArray(data) ? data : data.categories || [];
        setCategories(normalizeTree(raw));
      })
      .catch(() => toast('Failed to load categories', 'error'))
      .finally(() => setCatLoading(false));
  }, [locale]);

  useEffect(() => {
    getOperations()
      .then(data => setOperations(Array.isArray(data) ? data : data.items || data.operations || []))
      .catch(() => {});
  }, []);

  const fetchProducts = useCallback(async (pg = 1) => {
    setLoading(true);
    setError(null);
    try {
      const params = { ...filters, locale, page: pg, perPage: PER_PAGE };
      if (selectedNode) {
        if (selectedNode.type === 'section') params.sectionId = selectedNode.id;
        else if (selectedNode.type === 'category') params.categoryId = selectedNode.id;
        else if (selectedNode.type === 'group') params.groupId = selectedNode.id;
      }
      if (selectedOperation) params.operation = selectedOperation;
      const data = await getProducts(params);
      const items = Array.isArray(data) ? data : data.items || data.products || data.content || [];
      const totalCount = typeof data === 'object' && !Array.isArray(data)
        ? (data.total || data.totalElements || data.count || items.length)
        : items.length;
      setProducts(items);
      setTotal(totalCount);
    } catch (err) {
      setError(err.message);
      toast(err.message, 'error');
    } finally {
      setLoading(false);
    }
  }, [filters, locale, selectedNode, selectedOperation]);

  useEffect(() => {
    setPage(1);
    fetchProducts(1);
  }, [filters, locale, selectedNode, selectedOperation]);

  function handlePageChange(p) {
    setPage(p);
    fetchProducts(p);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function handleCategorySelect(node) {
    setSelectedNode(node);
    setPage(1);
  }

  function handleClearFilters() {
    setFilters(EMPTY_FILTERS);
  }

  // Search
  async function doSearch(q, pg = 1) {
    if (!q.trim()) {
      setSearchResults(null);
      return;
    }
    setSearchLoading(true);
    try {
      const data = await search(q, locale, pg, PER_PAGE);
      const items = Array.isArray(data) ? data : data.items || data.results || data.content || [];
      const totalCount = typeof data === 'object' && !Array.isArray(data)
        ? (data.total || data.totalElements || data.count || items.length)
        : items.length;
      setSearchResults(items);
      setSearchTotal(totalCount);
    } catch (err) {
      toast(err.message, 'error');
      setSearchResults(null);
    } finally {
      setSearchLoading(false);
    }
  }

  function handleSearchSubmit(e) {
    e.preventDefault();
    setSearchPage(1);
    doSearch(searchQuery, 1);
  }

  function handleSearchPageChange(pg) {
    setSearchPage(pg);
    doSearch(searchQuery, pg);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function clearSearch() {
    setSearchQuery('');
    setSearchResults(null);
    setSearchPage(1);
  }

  const isSearchMode = searchResults !== null;

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Product Catalog</h1>
        <p className="page-subtitle">Browse and filter our complete product range</p>
      </div>

      <div className="catalog-search-bar">
        <form className="search-bar" onSubmit={handleSearchSubmit}>
          <input
            ref={inputRef}
            className="form-control"
            type="search"
            placeholder="Search products by name, tool number, or description..."
            value={searchQuery}
            onChange={e => {
              setSearchQuery(e.target.value);
              if (!e.target.value.trim()) clearSearch();
            }}
          />
          <button type="submit" className="btn btn-primary">Search</button>
        </form>
      </div>

      {isSearchMode ? (
        <div className="catalog-search-results">
          <div className="catalog-search-results-header">
            <span style={{ fontSize: 13, color: 'var(--wpw-mid-gray)' }}>
              Found <strong style={{ color: 'var(--wpw-navy)' }}>{searchTotal}</strong> results for "{searchQuery}"
            </span>
            <button className="filters-clear-btn" onClick={clearSearch}>Back to catalog</button>
          </div>

          {searchLoading ? (
            <SkeletonGrid count={12} />
          ) : searchResults.length === 0 ? (
            <div className="empty-state">
              <h3>No results found</h3>
              <p>No products matched "{searchQuery}". Try a different search term.</p>
            </div>
          ) : (
            <>
              <div className="product-grid">
                {searchResults.map(p => (
                  <ProductCard key={p.id || p.toolNo || p.tool_no} product={p} />
                ))}
              </div>
              <Pagination
                page={searchPage}
                total={searchTotal}
                perPage={PER_PAGE}
                onChange={handleSearchPageChange}
              />
            </>
          )}
        </div>
      ) : (
        <>
          {operations.length > 0 && (
            <div className="operation-bar">
              {operations.map(op => {
                const code = op.code || op.id;
                const label = op.name || op.label || op.code || '';
                const isActive = selectedOperation === code;
                return (
                  <button
                    key={code}
                    className={`operation-chip${isActive ? ' active' : ''}`}
                    onClick={() => setSelectedOperation(isActive ? null : code)}
                  >
                    {label}
                  </button>
                );
              })}
            </div>
          )}

          <div className="catalog-layout">
            {/* Mobile filter overlay backdrop */}
            {mobileFiltersOpen && (
              <div className="mobile-filters-backdrop" onClick={() => setMobileFiltersOpen(false)} />
            )}

            <aside className={`catalog-sidebar${mobileFiltersOpen ? ' mobile-open' : ''}`}>
              <div className="mobile-filters-header">
                <span>Filters & Categories</span>
                <button className="mobile-filters-close" onClick={() => setMobileFiltersOpen(false)}>✕</button>
              </div>
              {!catLoading && (
                <CategoryTree
                  categories={categories}
                  selected={selectedNode}
                  onSelect={node => { handleCategorySelect(node); setMobileFiltersOpen(false); }}
                />
              )}
              <FiltersPanel
                filters={filters}
                onChange={setFilters}
                onClear={handleClearFilters}
              />
            </aside>

            <main className="catalog-main">
              <div className="catalog-toolbar">
                <div className="catalog-count">
                  {!loading && (
                    <span>
                      Showing <strong>{products.length}</strong> of <strong>{total}</strong> products
                    </span>
                  )}
                </div>
                <button
                  className="catalog-filters-toggle-btn"
                  onClick={() => setMobileFiltersOpen(true)}
                >
                  Filters
                </button>
              </div>

              {error && !loading && (
                <ErrorState message={error} onRetry={() => fetchProducts(page)} />
              )}

              {loading ? (
                <SkeletonGrid count={12} />
              ) : products.length === 0 && !error ? (
                <div className="empty-state">
                  <div className="empty-state-icon">📦</div>
                  <h3>No products found</h3>
                  <p>Try adjusting your filters or selecting a different category.</p>
                </div>
              ) : (
                <>
                  <div className="product-grid">
                    {products.map(p => (
                      <ProductCard key={p.id || p.toolNo || p.tool_no} product={p} />
                    ))}
                  </div>
                  <Pagination
                    page={page}
                    total={total}
                    perPage={PER_PAGE}
                    onChange={handlePageChange}
                  />
                </>
              )}
            </main>
          </div>
        </>
      )}
    </div>
  );
}
