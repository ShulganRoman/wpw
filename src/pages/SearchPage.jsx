import { useState, useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { search } from '../api/api';
import Pagination from '../components/Pagination';
import { LoadingSpinner, ErrorState } from '../components/LoadingState';
import { useToast } from '../components/ToastContext';

const PER_PAGE = 20;

const PLACEHOLDER_SVG = (
  <svg viewBox="0 0 72 72" width="72" height="72" fill="none" xmlns="http://www.w3.org/2000/svg">
    <rect width="72" height="72" rx="4" fill="#eceff1" />
    <path d="M18 50L28 34L38 46L46 36L56 50H18Z" fill="#b0bec5" />
    <circle cx="48" cy="24" r="8" fill="#b0bec5" />
  </svg>
);

export default function SearchPage({ locale }) {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const toast = useToast();
  const inputRef = useRef(null);

  const qParam = searchParams.get('q') || '';
  const pageParam = parseInt(searchParams.get('page') || '1', 10);

  const [query, setQuery] = useState(qParam);
  const [results, setResults] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(pageParam);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [searched, setSearched] = useState(false);

  useEffect(() => {
    if (qParam) {
      doSearch(qParam, pageParam);
      setQuery(qParam);
      setPage(pageParam);
    }
  }, [qParam, pageParam, locale]);

  async function doSearch(q, pg = 1) {
    if (!q.trim()) return;
    setLoading(true);
    setError(null);
    setSearched(true);
    try {
      const data = await search(q, locale, pg, PER_PAGE);
      const items = Array.isArray(data) ? data : data.items || data.results || data.content || [];
      const totalCount = typeof data === 'object' && !Array.isArray(data)
        ? (data.total || data.totalElements || data.count || items.length)
        : items.length;
      setResults(items);
      setTotal(totalCount);
    } catch (err) {
      setError(err.message);
      toast(err.message, 'error');
    } finally {
      setLoading(false);
    }
  }

  function handleSubmit(e) {
    e.preventDefault();
    if (!query.trim()) return;
    setPage(1);
    setSearchParams({ q: query, page: 1 });
    doSearch(query, 1);
  }

  function handlePageChange(pg) {
    setPage(pg);
    setSearchParams({ q: query, page: pg });
    doSearch(query, pg);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function goToProduct(toolNo) {
    navigate(`/product/${encodeURIComponent(toolNo)}`);
  }

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Search</h1>
        <p className="page-subtitle">Find products by name, tool number, or description</p>
      </div>

      <div className="search-bar-container">
        <form className="search-bar" onSubmit={handleSubmit}>
          <input
            ref={inputRef}
            className="form-control"
            type="search"
            placeholder="Search products…"
            value={query}
            onChange={e => setQuery(e.target.value)}
            autoFocus
          />
          <button type="submit" className="btn btn-primary">
            Search
          </button>
        </form>
      </div>

      {loading && <LoadingSpinner text="Searching…" />}

      {error && !loading && (
        <ErrorState message={error} onRetry={() => doSearch(qParam, page)} />
      )}

      {!loading && searched && results.length === 0 && !error && (
        <div className="empty-state">
          <div className="empty-state-icon">🔍</div>
          <h3>No results found</h3>
          <p>No products matched "{qParam}". Try a different search term.</p>
        </div>
      )}

      {!loading && results.length > 0 && (
        <>
          <div style={{ marginBottom: 16, fontSize: 13, color: 'var(--wpw-mid-gray)' }}>
            Found <strong style={{ color: 'var(--wpw-navy)' }}>{total}</strong> results for "{qParam}"
          </div>

          <div className="search-results-list">
            {results.map(item => {
              const toolNo = item.toolNo || item.tool_no || item.id;
              const name = item.name || item.productName || item.product_name || '';
              const desc = item.description || item.shortDescription || item.short_description || '';
              const imgUrl = item.thumbnailUrl || item.imageUrl || item.image_url || item.mainImageUrl;
              const stockKey = item.stockStatus || item.stock_status || 'out_of_stock';

              return (
                <div
                  key={item.id || toolNo}
                  className="search-result-item"
                  onClick={() => goToProduct(toolNo)}
                  role="button"
                  tabIndex={0}
                  onKeyDown={e => e.key === 'Enter' && goToProduct(toolNo)}
                >
                  {imgUrl ? (
                    <img className="search-result-img" src={imgUrl} alt={name} loading="lazy"
                      onError={e => { e.currentTarget.style.display = 'none'; }}
                    />
                  ) : (
                    <div className="search-result-img" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                      {PLACEHOLDER_SVG}
                    </div>
                  )}
                  <div className="search-result-body">
                    <div className="search-result-toolno">{toolNo}</div>
                    <div className="search-result-name">{name}</div>
                    {desc && <div className="search-result-desc">{desc}</div>}
                  </div>
                  <span className={`stock-badge ${stockKey}`} style={{ flexShrink: 0 }}>
                    {stockKey === 'in_stock' ? 'In Stock' : stockKey === 'low_stock' ? 'Low Stock' : 'Out of Stock'}
                  </span>
                </div>
              );
            })}
          </div>

          <Pagination
            page={page}
            total={total}
            perPage={PER_PAGE}
            onChange={handlePageChange}
          />
        </>
      )}

      {!searched && (
        <div className="empty-state">
          <div className="empty-state-icon">🔍</div>
          <h3>Start searching</h3>
          <p>Enter a keyword to search across all products.</p>
        </div>
      )}
    </div>
  );
}
