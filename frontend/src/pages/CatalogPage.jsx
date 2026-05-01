import {useState, useEffect, useCallback, useRef} from 'react';
import {getCategories, getProducts, getOperations, search, getFilterOptions, getCart, addToCart, addToCartByFilter} from '../api/api';
import ProductCard from '../components/ProductCard';
import CartSidebar from '../components/CartSidebar';
import Pagination from '../components/Pagination';
import {SkeletonGrid, ErrorState} from '../components/LoadingState';
import {useToast} from '../components/ToastContext';
import {useCatalogSession} from '../contexts/SessionContext';

const PER_PAGE = 50;


function CategoryTree({categories, selected, onSelect}) {
    const [expanded, setExpanded] = useState({});

    function toggle(id) {
        setExpanded(prev => ({...prev, [id]: !prev[id]}));
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
                        onClick={() => onSelect(isSelected ? null : {type: node.type, id: node.id, imageUrl: node.imageUrl})}
                    >
                        {node.imageUrl && (
                            <img src={node.imageUrl} alt="" className="category-tree-thumb" />
                        )}
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
                        <span style={{width: 36, flexShrink: 0}}/>
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
                        <button className="category-tree-select category-tree-select--all"
                                onClick={() => onSelect(null)}>
                            <span className="category-tree-text">All Products</span>
                        </button>
                        <span style={{width: 36, flexShrink: 0}}/>
                    </div>
                </div>
                {categories.map(cat => renderNode(cat))}
            </div>
        </div>
    );
}

function FilterSelect({label, filterKey, options, value, onChange}) {
    return (
        <div className="form-group">
            <label className="form-label">{label}</label>
            <select
                className="form-control"
                value={value || ''}
                onChange={e => onChange(filterKey, e.target.value)}
            >
                <option value="">All</option>
                {options.map(opt => (
                    <option key={opt} value={opt}>{opt}</option>
                ))}
            </select>
        </div>
    );
}

function FiltersPanel({filters, filterOptions, onChange, onClear}) {
    function handleChange(key, value) {
        onChange({...filters, [key]: value});
    }

    const hasActive = Object.values(filters).some(v => v !== '');
    const role = localStorage.getItem('userRole');
    const hasPrices = role === 'admin' || role === 'dealer';

    return (
        <div className="filters-panel">
            <div className="filters-header">
                <span>Filters</span>
                {hasActive && (
                    <button className="filters-clear-btn" onClick={onClear}>Clear all</button>
                )}
            </div>
            <div className="filters-body">
                <FilterSelect
                    label="Tool Material"
                    filterKey="toolMaterial"
                    options={filterOptions.toolMaterial || []}
                    value={filters.toolMaterial}
                    onChange={handleChange}
                />
                <FilterSelect
                    label="Workpiece Material"
                    filterKey="workpieceMaterial"
                    options={filterOptions.workpieceMaterial || []}
                    value={filters.workpieceMaterial}
                    onChange={handleChange}
                />
                <FilterSelect
                    label="Machine Type"
                    filterKey="machineType"
                    options={filterOptions.machineType || []}
                    value={filters.machineType}
                    onChange={handleChange}
                />
                <FilterSelect
                    label="Machine Brand"
                    filterKey="machineBrand"
                    options={filterOptions.machineBrand || []}
                    value={filters.machineBrand}
                    onChange={handleChange}
                />
                <FilterSelect
                    label="Cutting Type"
                    filterKey="cuttingType"
                    options={filterOptions.cuttingType || []}
                    value={filters.cuttingType}
                    onChange={handleChange}
                />
                <FilterSelect
                    label="Shank (mm)"
                    filterKey="shankMm"
                    options={filterOptions.shankMm || []}
                    value={filters.shankMm}
                    onChange={handleChange}
                />

                <div className="form-group">
                    <label className="form-label">Diameter (mm)</label>
                    <div className="filter-range">
                        <input
                            className="form-control"
                            type="number"
                            placeholder="Min"
                            min="0"
                            value={filters.dMmMin || ''}
                            onChange={e => handleChange('dMmMin', e.target.value)}
                        />
                        <input
                            className="form-control"
                            type="number"
                            placeholder="Max"
                            min="0"
                            value={filters.dMmMax || ''}
                            onChange={e => handleChange('dMmMax', e.target.value)}
                        />
                    </div>
                </div>

                {hasPrices && (
                    <div className="form-group">
                        <label className="form-label">Price</label>
                        <div className="filter-range">
                            <input
                                className="form-control"
                                type="number"
                                placeholder="Min"
                                min="0"
                                value={filters.priceMin || ''}
                                onChange={e => handleChange('priceMin', e.target.value)}
                            />
                            <input
                                className="form-control"
                                type="number"
                                placeholder="Max"
                                min="0"
                                value={filters.priceMax || ''}
                                onChange={e => handleChange('priceMax', e.target.value)}
                            />
                        </div>
                    </div>
                )}

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
                    <select
                        className="form-control"
                        value={filters.productType || ''}
                        onChange={e => handleChange('productType', e.target.value)}
                    >
                        <option value="">All</option>
                        <option value="main">Main</option>
                        <option value="spare_part">Spare Part</option>
                        <option value="accessory">Accessory</option>
                    </select>
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
    productType: '', inStock: '', priceMin: '', priceMax: '',
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


export default function CatalogPage({locale}) {
    const toast = useToast();
    const isDealer = localStorage.getItem('userRole') === 'dealer';

    // Persistent session state (survives navigation)
    const {
        selected, setSelected,
        filters, setFilters,
        selectedNode, setSelectedNode,
        selectedOperation, setSelectedOperation,
        page, setPage,
        searchQuery, setSearchQuery,
        searchPage, setSearchPage,
        searchResults, setSearchResults,
        searchTotal, setSearchTotal,
    } = useCatalogSession();

    // Local state (server data / transient UI)
    const [categories, setCategories] = useState([]);
    const [products, setProducts] = useState([]);
    const [total, setTotal] = useState(0);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [catLoading, setCatLoading] = useState(true);
    const [operations, setOperations] = useState([]);
    const [filterOptions, setFilterOptions] = useState({});
    const [mobileFiltersOpen, setMobileFiltersOpen] = useState(false);
    const [searchLoading, setSearchLoading] = useState(false);

    // Cart state (dealer only)
    const [cartOpen, setCartOpen] = useState(false);
    const [cartData, setCartData] = useState(null);
    const [addingToCart, setAddingToCart] = useState(false);

    // Drag-to-select
    const dragRef = useRef({ active: false, action: 'add' });
    const inputRef = useRef(null);

    useEffect(() => {
        function onMouseUp() { dragRef.current.active = false; }
        document.addEventListener('mouseup', onMouseUp);
        return () => document.removeEventListener('mouseup', onMouseUp);
    }, []);

    function handleDragStart(id, isSelected) {
        dragRef.current.active = true;
        dragRef.current.action = isSelected ? 'remove' : 'add';
        setSelected(prev => {
            const next = new Set(prev);
            if (dragRef.current.action === 'add') next.add(id); else next.delete(id);
            return next;
        });
    }

    function handleDragMove(id) {
        if (!dragRef.current.active) return;
        setSelected(prev => {
            const next = new Set(prev);
            if (dragRef.current.action === 'add') next.add(id); else next.delete(id);
            return next;
        });
    }

    useEffect(() => {
        getFilterOptions().then(setFilterOptions).catch(() => {});
    }, []);

    useEffect(() => {
        if (!isDealer) return;
        getCart().then(setCartData).catch(() => {});
    }, [isDealer]);

    async function handleAddToCart(productIds) {
        if (!productIds.length) return;
        setAddingToCart(true);
        try {
            const cart = await addToCart(productIds);
            setCartData(cart);
            setSelected(new Set());
            toast(`Added ${productIds.length} item(s) to cart`, 'success');
        } catch (err) {
            toast(err.message, 'error');
        } finally {
            setAddingToCart(false);
        }
    }

    async function handleAddAllByFilter() {
        setAddingToCart(true);
        try {
            const params = {...filters, locale};
            if (selectedNode) {
                if (selectedNode.type === 'section') params.sectionId = selectedNode.id;
                else if (selectedNode.type === 'category') params.categoryId = selectedNode.id;
                else if (selectedNode.type === 'group') params.groupId = selectedNode.id;
            }
            if (selectedOperation) params.operation = selectedOperation;
            const cart = await addToCartByFilter(params);
            setCartData(cart);
            toast(`Added all ${total} matching products to cart`, 'success');
        } catch (err) {
            toast(err.message, 'error');
        } finally {
            setAddingToCart(false);
        }
    }

    function toggleSelect(id) {
        setSelected(prev => {
            const next = new Set(prev);
            if (next.has(id)) next.delete(id); else next.add(id);
            return next;
        });
    }

    function toggleSelectAll() {
        const pageItems = searchResults !== null ? (searchResults || []) : products;
        const allPageSelected = pageItems.length > 0 && pageItems.every(p => selected.has(p.id));
        setSelected(prev => {
            const next = new Set(prev);
            if (allPageSelected) pageItems.forEach(p => next.delete(p.id));
            else pageItems.forEach(p => next.add(p.id));
            return next;
        });
    }

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
            .catch(() => {
            });
    }, []);

    const fetchProducts = useCallback(async (pg = 1) => {
        setLoading(true);
        setError(null);
        try {
            const params = {...filters, locale, page: pg, perPage: PER_PAGE};
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
        window.scrollTo({top: 0, behavior: 'smooth'});
    }

    function handleCategorySelect(node) {
        setSelectedNode(node);
        setPage(1);
        setSelected(new Set());
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
        window.scrollTo({top: 0, behavior: 'smooth'});
    }

    function clearSearch() {
        setSearchQuery('');
        setSearchResults(null);
        setSearchPage(1);
    }

    const isSearchMode = searchResults !== null;
    const displayProducts = isSearchMode ? (searchResults || []) : products;
    const displayTotal    = isSearchMode ? searchTotal : total;
    const displayPage     = isSearchMode ? searchPage  : page;
    const displayLoading  = isSearchMode ? searchLoading : loading;
    const handleDisplayPageChange = isSearchMode ? handleSearchPageChange : handlePageChange;

    const cartItemCount = cartData?.totalItems || cartData?.items?.length || 0;
    const cartProductIds = new Set((cartData?.items || []).map(it => it.productId));

    return (
        <div>
            <div className="page-header" style={{display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12}}>
                <div>
                    <h1 className="page-title">Product Catalog</h1>
                    <p className="page-subtitle">Browse and filter our complete product range</p>
                </div>
                {isDealer && (
                    <button
                        className="btn btn-secondary"
                        style={{position: 'relative', flexShrink: 0}}
                        onClick={() => setCartOpen(true)}
                    >
                        🛒 Cart
                        {cartItemCount > 0 && (
                            <span style={{
                                position: 'absolute', top: -6, right: -6,
                                background: 'var(--wpw-accent)', color: '#fff',
                                borderRadius: 10, fontSize: 10, padding: '1px 6px', fontWeight: 700,
                            }}>{cartItemCount}</span>
                        )}
                    </button>
                )}
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

            {!isSearchMode && operations.length > 0 && (
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
                {mobileFiltersOpen && (
                    <div className="mobile-filters-backdrop" onClick={() => setMobileFiltersOpen(false)}/>
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
                            onSelect={node => {
                                handleCategorySelect(node);
                                setMobileFiltersOpen(false);
                            }}
                        />
                    )}
                    <FiltersPanel
                        filters={filters}
                        filterOptions={filterOptions}
                        onChange={setFilters}
                        onClear={handleClearFilters}
                    />
                </aside>

                <main className="catalog-main">
                    {/* Search mode banner */}
                    {isSearchMode && (
                        <div style={{
                            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                            padding: '6px 10px', marginBottom: 8,
                            background: '#e8f0fe', borderRadius: 6,
                            border: '1px solid #c5d4f6', fontSize: 13,
                        }}>
                            <span>
                                Found <strong>{searchTotal}</strong> results for &ldquo;{searchQuery}&rdquo;
                            </span>
                            <button className="filters-clear-btn" onClick={clearSearch}>✕ Clear search</button>
                        </div>
                    )}

                    <div className="catalog-toolbar">
                        <div className="catalog-count">
                            {!displayLoading && (
                                <span>
                                    Showing <strong>{displayProducts.length}</strong> of <strong>{displayTotal}</strong> products
                                </span>
                            )}
                        </div>
                        <div style={{display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap'}}>
                            {isDealer && selected.size > 0 && (
                                <button
                                    className="btn btn-primary"
                                    style={{fontSize: 13, padding: '6px 12px'}}
                                    disabled={addingToCart}
                                    onClick={() => handleAddToCart([...selected])}
                                >
                                    {addingToCart ? 'Adding…' : `🛒 Add selected (${selected.size})`}
                                </button>
                            )}
                            {isDealer && !isSearchMode && displayTotal > 0 && (
                                <button
                                    className="btn btn-secondary"
                                    style={{fontSize: 13, padding: '6px 12px'}}
                                    disabled={addingToCart}
                                    onClick={handleAddAllByFilter}
                                >
                                    {addingToCart ? 'Adding…' : `🛒 Add all (${displayTotal})`}
                                </button>
                            )}
                            <button
                                className="catalog-filters-toggle-btn"
                                onClick={() => setMobileFiltersOpen(true)}
                            >
                                Filters
                            </button>
                        </div>
                    </div>

                    {isDealer && displayProducts.length > 0 && !displayLoading && (
                        <div style={{
                            display: 'flex', alignItems: 'center', gap: 8,
                            padding: '6px 10px', marginBottom: 8,
                            background: 'var(--wpw-surface)', borderRadius: 6,
                            border: '1px solid var(--wpw-border)',
                        }}>
                            <input
                                type="checkbox"
                                checked={displayProducts.every(p => selected.has(p.id))}
                                onChange={toggleSelectAll}
                                style={{cursor: 'pointer'}}
                            />
                            <span style={{fontSize: 13, color: 'var(--wpw-text-secondary)'}}>
                                {selected.size > 0
                                    ? `${selected.size} selected (this page: ${displayProducts.filter(p => selected.has(p.id)).length})`
                                    : `Select all on this page (${displayProducts.length})`}
                            </span>
                            {selected.size > 0 && (
                                <button
                                    style={{
                                        marginLeft: 'auto', fontSize: 12,
                                        background: 'none', border: 'none',
                                        color: 'var(--wpw-text-secondary)', cursor: 'pointer',
                                    }}
                                    onClick={() => setSelected(new Set())}
                                >
                                    Clear selection
                                </button>
                            )}
                        </div>
                    )}

                    {!isSearchMode && error && !displayLoading && (
                        <ErrorState message={error} onRetry={() => fetchProducts(page)}/>
                    )}

                    {displayLoading ? (
                        <SkeletonGrid count={12}/>
                    ) : displayProducts.length === 0 ? (
                        <div className="empty-state">
                            <div className="empty-state-icon">📦</div>
                            <h3>{isSearchMode ? 'No results found' : 'No products found'}</h3>
                            <p>{isSearchMode
                                ? `No products matched "${searchQuery}". Try a different search term.`
                                : 'Try adjusting your filters or selecting a different category.'
                            }</p>
                        </div>
                    ) : (
                        <>
                            <div className="product-grid" style={{userSelect: 'none'}}>
                                {displayProducts.map(p => (
                                    <ProductCard
                                        key={p.id || p.toolNo || p.tool_no}
                                        product={p}
                                        selectable={isDealer}
                                        selected={isDealer && selected.has(p.id)}
                                        inCart={isDealer && cartProductIds.has(p.id)}
                                        onSelect={() => toggleSelect(p.id)}
                                        onAddToCart={isDealer ? prod => handleAddToCart([prod.id]) : undefined}
                                        onDragStart={isDealer ? (isSelected) => handleDragStart(p.id, isSelected) : undefined}
                                        onDragMove={isDealer ? () => handleDragMove(p.id) : undefined}
                                    />
                                ))}
                            </div>
                            <Pagination
                                page={displayPage}
                                total={displayTotal}
                                perPage={PER_PAGE}
                                onChange={handleDisplayPageChange}
                            />
                        </>
                    )}
                </main>
            </div>

            {isDealer && (
                <CartSidebar
                    open={cartOpen}
                    onClose={() => setCartOpen(false)}
                    cartData={cartData}
                    onCartUpdate={setCartData}
                />
            )}
        </div>
    );
}
