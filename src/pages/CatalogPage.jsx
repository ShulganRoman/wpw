import { useState, useEffect, useCallback } from 'react';
import { getCategories, getProducts } from '../api/api';
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
    const isSelected = selected === node.id;

    return (
      <div key={node.id} className="category-tree-item" style={{ paddingLeft: depth > 0 ? 0 : 0 }}>
        <button
          className={`category-tree-btn${isSelected ? ' active' : ''}`}
          onClick={() => { onSelect(isSelected ? null : node.id); }}
        >
          {hasChildren && (
            <span
              className={`category-chevron${isExpanded ? ' open' : ''}`}
              onClick={e => { e.stopPropagation(); toggle(node.id); }}
            >
              ▶
            </span>
          )}
          {!hasChildren && <span style={{ width: 14, flexShrink: 0 }} />}
          <span style={{ flex: 1, textAlign: 'left' }}>{node.name || node.label}</span>
        </button>
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
      <div className="category-tree-header">Categories</div>
      <div className="category-tree-list">
        <div className="category-tree-item">
          <button
            className={`category-tree-btn all${!selected ? ' active' : ''}`}
            onClick={() => onSelect(null)}
          >
            <span style={{ width: 14, flexShrink: 0 }} />
            All Products
          </button>
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

export default function CatalogPage({ locale }) {
  const toast = useToast();
  const [categories, setCategories] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState(null);
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [products, setProducts] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [catLoading, setCatLoading] = useState(true);

  useEffect(() => {
    setCatLoading(true);
    getCategories(locale)
      .then(data => setCategories(Array.isArray(data) ? data : data.categories || []))
      .catch(() => toast('Failed to load categories', 'error'))
      .finally(() => setCatLoading(false));
  }, [locale]);

  const fetchProducts = useCallback(async (pg = 1) => {
    setLoading(true);
    setError(null);
    try {
      const params = { ...filters, locale, page: pg, perPage: PER_PAGE };
      if (selectedCategory) params.categoryId = selectedCategory;
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
  }, [filters, locale, selectedCategory]);

  useEffect(() => {
    setPage(1);
    fetchProducts(1);
  }, [filters, locale, selectedCategory]);

  function handlePageChange(p) {
    setPage(p);
    fetchProducts(p);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function handleCategorySelect(id) {
    setSelectedCategory(id);
    setPage(1);
  }

  function handleClearFilters() {
    setFilters(EMPTY_FILTERS);
  }

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Product Catalog</h1>
        <p className="page-subtitle">Browse and filter our complete product range</p>
      </div>

      <div className="catalog-layout">
        <aside className="catalog-sidebar">
          {!catLoading && (
            <CategoryTree
              categories={categories}
              selected={selectedCategory}
              onSelect={handleCategorySelect}
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
    </div>
  );
}
