import { useState, useEffect } from 'react';
import { getOperations, getOperationProducts } from '../api/api';
import ProductCard from '../components/ProductCard';
import Pagination from '../components/Pagination';
import { LoadingSpinner, SkeletonGrid, ErrorState } from '../components/LoadingState';
import { useToast } from '../components/ToastContext';

const PER_PAGE = 48;

const OPERATION_ICONS = {
  drilling: '🔩',
  milling: '⚙️',
  turning: '🔄',
  grinding: '💎',
  cutting: '✂️',
  boring: '🎯',
  reaming: '🔧',
  tapping: '🔗',
  broaching: '📐',
  default: '🛠️',
};

function getIcon(code, label) {
  const key = (code || label || '').toLowerCase();
  for (const [k, v] of Object.entries(OPERATION_ICONS)) {
    if (key.includes(k)) return v;
  }
  return OPERATION_ICONS.default;
}

export default function OperationsPage({ locale }) {
  const toast = useToast();

  const [operations, setOperations] = useState([]);
  const [selectedOp, setSelectedOp] = useState(null);
  const [products, setProducts] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [opLoading, setOpLoading] = useState(true);
  const [prodLoading, setProdLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    setOpLoading(true);
    getOperations()
      .then(data => setOperations(Array.isArray(data) ? data : data.items || data.operations || []))
      .catch(err => {
        toast(err.message, 'error');
        setError(err.message);
      })
      .finally(() => setOpLoading(false));
  }, []);

  async function selectOperation(op) {
    if (selectedOp?.code === op.code) {
      setSelectedOp(null);
      setProducts([]);
      return;
    }
    setSelectedOp(op);
    setPage(1);
    await fetchOpProducts(op.code, 1);
  }

  async function fetchOpProducts(code, pg) {
    setProdLoading(true);
    try {
      const data = await getOperationProducts(code, locale, pg, PER_PAGE);
      const items = Array.isArray(data) ? data : data.items || data.products || data.content || [];
      const totalCount = typeof data === 'object' && !Array.isArray(data)
        ? (data.total || data.totalElements || data.count || items.length)
        : items.length;
      setProducts(items);
      setTotal(totalCount);
    } catch (err) {
      toast(err.message, 'error');
    } finally {
      setProdLoading(false);
    }
  }

  function handlePageChange(pg) {
    setPage(pg);
    fetchOpProducts(selectedOp.code, pg);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  if (opLoading) return <LoadingSpinner text="Loading application tags…" />;
  if (error && operations.length === 0) return <ErrorState message={error} />;

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Application Tags</h1>
        <p className="page-subtitle">Browse products by application</p>
      </div>

      <div className="operations-grid">
        {operations.map(op => {
          const code = op.code || op.id;
          const label = op.name || op.label || op.code || '';
          const count = op.productCount || op.count || '';
          const isActive = selectedOp?.code === code;

          return (
            <div
              key={code}
              className={`operation-card${isActive ? ' active' : ''}`}
              onClick={() => selectOperation({ ...op, code })}
              role="button"
              tabIndex={0}
              onKeyDown={e => e.key === 'Enter' && selectOperation({ ...op, code })}
            >
              <div className="operation-icon">{getIcon(code, label)}</div>
              <div className="operation-name">{label}</div>
              {count !== '' && (
                <div className="operation-count">{count} products</div>
              )}
            </div>
          );
        })}
      </div>

      {selectedOp && (
        <div>
          <div className="page-header">
            <h2 className="page-title" style={{ fontSize: 18 }}>
              {getIcon(selectedOp.code, selectedOp.name || selectedOp.code)} {selectedOp.name || selectedOp.label || selectedOp.code}
            </h2>
            {!prodLoading && (
              <p className="page-subtitle">{total} products</p>
            )}
          </div>

          {prodLoading ? (
            <SkeletonGrid count={12} />
          ) : products.length === 0 ? (
            <div className="empty-state">
              <div className="empty-state-icon">📦</div>
              <h3>No products found</h3>
              <p>No products are associated with this operation.</p>
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
        </div>
      )}
    </div>
  );
}
