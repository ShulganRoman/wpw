import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getCart, updateCartQty, removeFromCart, clearCart, checkout, getDealerOrders, getDealerOrder } from '../api/api';
import { LoadingSpinner } from '../components/LoadingState';
import { useToast } from '../components/ToastContext';

const PAGE_SIZE = 10;

function QtyControl({ qty, onChange, disabled }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
      <button
        onClick={() => onChange(qty - 1)}
        disabled={disabled}
        style={{
          width: 28, height: 28, border: '1px solid var(--wpw-border)', borderRadius: 4,
          background: 'var(--wpw-surface)', cursor: disabled ? 'not-allowed' : 'pointer', fontSize: 16,
        }}
      >−</button>
      <input
        type="number" min="1" value={qty}
        onChange={e => onChange(parseInt(e.target.value, 10) || 1)}
        disabled={disabled}
        style={{
          width: 52, textAlign: 'center', border: '1px solid var(--wpw-border)',
          borderRadius: 4, padding: '4px', fontSize: 14,
        }}
      />
      <button
        onClick={() => onChange(qty + 1)}
        disabled={disabled}
        style={{
          width: 28, height: 28, border: '1px solid var(--wpw-border)', borderRadius: 4,
          background: 'var(--wpw-surface)', cursor: disabled ? 'not-allowed' : 'pointer', fontSize: 16,
        }}
      >+</button>
    </div>
  );
}

function PriceTiers({ tiers, qty, currency }) {
  if (!tiers || tiers.length === 0) return <span style={{ fontSize: 12, color: 'var(--wpw-text-secondary)' }}>No price</span>;
  return (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4 }}>
      {tiers.map(t => {
        const active = qty >= t.minQty && (!tiers.find(x => x.minQty > t.minQty && qty >= x.minQty));
        return (
          <span key={t.minQty} style={{
            fontSize: 11, padding: '2px 6px', borderRadius: 4,
            background: active ? 'var(--wpw-accent)' : 'var(--wpw-surface)',
            color: active ? '#fff' : 'var(--wpw-text-secondary)',
            border: `1px solid ${active ? 'var(--wpw-accent)' : 'var(--wpw-border)'}`,
            fontWeight: active ? 600 : 400,
          }}>
            {t.minQty}+ = {Number(t.price).toFixed(2)}
          </span>
        );
      })}
    </div>
  );
}

function CartRow({ item, onUpdate }) {
  const toast = useToast();
  const [qty, setQty] = useState(item.qty);
  const [updating, setUpdating] = useState(false);

  useEffect(() => { setQty(item.qty); }, [item.qty]);

  async function applyQty(newQty) {
    if (newQty < 1) { await handleRemove(); return; }
    setUpdating(true);
    try {
      const cart = await updateCartQty(item.productId, newQty);
      onUpdate(cart);
    } catch (err) {
      toast(err.message, 'error');
    } finally {
      setUpdating(false);
    }
  }

  async function handleRemove() {
    setUpdating(true);
    try {
      const cart = await removeFromCart(item.productId);
      onUpdate(cart);
    } catch (err) {
      toast(err.message, 'error');
    } finally {
      setUpdating(false);
    }
  }

  // find which tier is next (to show hint)
  const sortedTiers = [...(item.tiers || [])].sort((a, b) => a.minQty - b.minQty);
  const nextTier = sortedTiers.find(t => t.minQty > qty);

  return (
    <tr style={{ opacity: updating ? 0.6 : 1, transition: 'opacity 0.15s', verticalAlign: 'top' }}>
      <td style={{ padding: '12px 16px' }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', gap: 10 }}>
          {item.imageUrl && (
            <img src={item.imageUrl} alt={item.toolNo}
              style={{ width: 52, height: 52, objectFit: 'contain', borderRadius: 4, border: '1px solid var(--wpw-border)', flexShrink: 0 }} />
          )}
          <div style={{ minWidth: 0 }}>
            <div style={{ fontSize: 11, fontFamily: 'var(--wpw-font-mono)', color: 'var(--wpw-text-secondary)' }}>{item.toolNo}</div>
            <div style={{ fontSize: 14, fontWeight: 500, marginBottom: 4 }}>{item.name}</div>
            <PriceTiers tiers={sortedTiers} qty={qty} />
            {nextTier && (
              <div style={{ fontSize: 11, color: 'var(--wpw-accent)', marginTop: 3 }}>
                Add {nextTier.minQty - qty} more → {Number(nextTier.price).toFixed(2)}/pc
              </div>
            )}
          </div>
        </div>
      </td>
      <td style={{ textAlign: 'center', padding: '12px 8px', verticalAlign: 'middle' }}>
        <QtyControl qty={qty} disabled={updating} onChange={n => { setQty(n); applyQty(n); }} />
      </td>
      <td style={{ textAlign: 'right', fontSize: 14, padding: '12px 8px', verticalAlign: 'middle' }}>
        {item.unitPrice != null ? Number(item.unitPrice).toFixed(2) : '—'}
      </td>
      <td style={{ textAlign: 'right', fontWeight: 600, fontSize: 14, padding: '12px 8px', verticalAlign: 'middle' }}>
        {item.lineTotal != null ? Number(item.lineTotal).toFixed(2) : '—'}
      </td>
      <td style={{ textAlign: 'center', padding: '12px 8px', verticalAlign: 'middle' }}>
        <button
          onClick={handleRemove}
          disabled={updating}
          style={{ background: 'none', border: 'none', color: 'var(--wpw-error, #e53e3e)', cursor: 'pointer', fontSize: 18 }}
        >✕</button>
      </td>
    </tr>
  );
}

function exportCSV(items, currency, notes) {
  const rows = [
    ['Tool No', 'Product Name', 'Qty', 'Unit Price', 'Line Total', 'Currency'],
    ...items.map(it => [
      it.toolNo, it.name, it.qty,
      it.unitPrice != null ? Number(it.unitPrice).toFixed(2) : '',
      it.lineTotal != null ? Number(it.lineTotal).toFixed(2) : '',
      currency,
    ]),
  ];
  if (notes.trim()) {
    rows.push([]);
    rows.push(['Notes', notes]);
  }
  const csv = rows.map(r => r.map(v => `"${String(v).replace(/"/g, '""')}"`).join(',')).join('\n');
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `order-${new Date().toISOString().slice(0, 10)}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}

const DEALER_STATUS_LABELS = {
  SUBMITTED:     { label: 'Submitted',    color: '#1565c0', bg: '#e3f2fd' },
  IN_PROCESSING: { label: 'In Processing', color: '#e65100', bg: '#fff3e0' },
  CONFIRMED:     { label: 'Confirmed',    color: '#2e7d32', bg: '#e8f5e9' },
  REJECTED:      { label: 'Rejected',     color: '#c62828', bg: '#ffebee' },
};

function DealerStatusBadge({ status }) {
  const s = DEALER_STATUS_LABELS[status] || { label: status, color: '#555', bg: '#f5f5f5' };
  return (
    <span style={{ display: 'inline-block', padding: '2px 8px', borderRadius: 4, fontSize: 12, fontWeight: 600, color: s.color, background: s.bg }}>
      {s.label}
    </span>
  );
}

function OrderDetailDrawer({ orderId, onClose }) {
  const toast = useToast();
  const [order, setOrder] = useState(null);

  useEffect(() => {
    getDealerOrder(orderId)
      .then(setOrder)
      .catch(() => toast('Failed to load order', 'error'));
  }, [orderId]);

  return (
    <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.35)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }} onClick={onClose}>
      <div style={{ background: '#fff', borderRadius: 8, padding: 24, maxWidth: 680, width: '100%', maxHeight: '85vh', overflowY: 'auto', boxShadow: '0 8px 32px rgba(0,0,0,0.18)' }} onClick={e => e.stopPropagation()}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
          <div style={{ fontSize: 16, fontWeight: 700 }}>
            Order #{order?.id?.slice(0, 8).toUpperCase() || '...'}
          </div>
          <button onClick={onClose} style={{ background: 'none', border: 'none', fontSize: 20, cursor: 'pointer', color: '#999' }}>×</button>
        </div>
        {!order && <div style={{ color: '#888', textAlign: 'center', padding: 40 }}>Loading...</div>}
        {order && (
          <>
            <div style={{ display: 'flex', gap: 16, marginBottom: 20, flexWrap: 'wrap' }}>
              <div style={{ fontSize: 13 }}>Status: <DealerStatusBadge status={order.status} /></div>
              <div style={{ fontSize: 13 }}>Total: <strong>{Number(order.total).toFixed(2)} {order.currency}</strong></div>
              <div style={{ fontSize: 13, color: '#888' }}>{new Date(order.submittedAt).toLocaleString('ru-RU')}</div>
            </div>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
              <thead>
                <tr style={{ borderBottom: '2px solid var(--wpw-border)' }}>
                  <th style={{ padding: '6px 8px', textAlign: 'left' }}>SKU</th>
                  <th style={{ padding: '6px 8px', textAlign: 'left' }}>Name</th>
                  <th style={{ padding: '6px 8px', textAlign: 'right' }}>Qty</th>
                  <th style={{ padding: '6px 8px', textAlign: 'right' }}>Price</th>
                  <th style={{ padding: '6px 8px', textAlign: 'right' }}>Total</th>
                </tr>
              </thead>
              <tbody>
                {order.items.map(item => (
                  <tr key={item.toolNo} style={{ borderBottom: '1px solid var(--wpw-border)' }}>
                    <td style={{ padding: '6px 8px', fontFamily: 'monospace' }}>{item.toolNo}</td>
                    <td style={{ padding: '6px 8px' }}>{item.name}</td>
                    <td style={{ padding: '6px 8px', textAlign: 'right' }}>{item.qty}</td>
                    <td style={{ padding: '6px 8px', textAlign: 'right' }}>{item.unitPrice != null ? Number(item.unitPrice).toFixed(2) : '—'}</td>
                    <td style={{ padding: '6px 8px', textAlign: 'right', fontWeight: 600 }}>{item.lineTotal != null ? Number(item.lineTotal).toFixed(2) : '—'}</td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr>
                  <td colSpan={4} style={{ padding: '8px 8px', textAlign: 'right', fontWeight: 700 }}>Total:</td>
                  <td style={{ padding: '8px 8px', textAlign: 'right', fontWeight: 700 }}>{Number(order.total).toFixed(2)} {order.currency}</td>
                </tr>
              </tfoot>
            </table>
          </>
        )}
      </div>
    </div>
  );
}

function OrdersSection() {
  const toast = useToast();
  const [orders, setOrders] = useState(null);
  const [loading, setLoading] = useState(true);
  const [selectedId, setSelectedId] = useState(null);

  useEffect(() => {
    getDealerOrders()
      .then(setOrders)
      .catch(() => toast('Failed to load order history', 'error'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div style={{ padding: 20, color: '#888', fontSize: 13 }}>Loading...</div>;
  if (!orders || orders.length === 0) return (
    <div style={{ padding: '40px 0', textAlign: 'center', color: '#888', fontSize: 14 }}>
      You have no orders yet
    </div>
  );

  return (
    <div>
      <div style={{ overflowX: 'auto' }}>
        <table className="data-table">
          <thead>
            <tr>
              <th>#</th>
              <th>Date</th>
              <th>Status</th>
              <th style={{ textAlign: 'right' }}>Items</th>
              <th style={{ textAlign: 'right' }}>Total</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {orders.map(o => (
              <tr key={o.id}>
                <td style={{ fontFamily: 'var(--wpw-font-mono)', color: '#888', fontSize: 12 }}>
                  {o.id.slice(0, 8).toUpperCase()}
                </td>
                <td style={{ fontSize: 13 }}>{new Date(o.submittedAt).toLocaleString('ru-RU')}</td>
                <td><DealerStatusBadge status={o.status} /></td>
                <td style={{ textAlign: 'right' }}>{o.itemCount}</td>
                <td style={{ textAlign: 'right', fontWeight: 600 }}>
                  {Number(o.total).toFixed(2)} {o.currency}
                </td>
                <td>
                  <button className="btn btn-secondary" style={{ padding: '3px 10px', fontSize: 12 }} onClick={() => setSelectedId(o.id)}>
                    Open
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {selectedId && <OrderDetailDrawer orderId={selectedId} onClose={() => setSelectedId(null)} />}
    </div>
  );
}

export default function DealerPage() {
  const toast = useToast();
  const navigate = useNavigate();
  const isDealer = localStorage.getItem('userRole') === 'dealer';

  const [activeTab, setActiveTab] = useState('cart');
  const [cartData, setCartData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [checkingOut, setCheckingOut] = useState(false);
  const [notes, setNotes] = useState('');
  const [page, setPage] = useState(1);

  useEffect(() => {
    if (!isDealer) { setLoading(false); return; }
    getCart()
      .then(setCartData)
      .catch(err => toast(err.message, 'error'))
      .finally(() => setLoading(false));
  }, []);

  function handleCartUpdate(cart) {
    setCartData(cart);
    // reset to page 1 if current page becomes empty after removal
    const newTotal = cart?.items?.length || 0;
    const maxPage = Math.max(1, Math.ceil(newTotal / PAGE_SIZE));
    if (page > maxPage) setPage(maxPage);
  }

  async function handleClear() {
    try {
      await clearCart();
      const fresh = await getCart();
      setCartData(fresh);
      setPage(1);
    } catch (err) {
      toast(err.message, 'error');
    }
  }

  async function handleCheckout() {
    setCheckingOut(true);
    try {
      const res = await checkout();
      toast(`Order submitted: #${res.orderId?.toString().slice(0, 8).toUpperCase()}`, 'success');
      const fresh = await getCart();
      setCartData(fresh);
      setPage(1);
      setActiveTab('orders');
    } catch (err) {
      toast(err.message, 'error');
    } finally {
      setCheckingOut(false);
    }
  }

  if (!isDealer) {
    return (
      <div className="empty-state" style={{ marginTop: 60 }}>
        <div className="empty-state-icon">🔒</div>
        <h3>Dealer access only</h3>
        <p>This page is available for registered dealers only.</p>
      </div>
    );
  }

  if (loading) return <LoadingSpinner text="Loading order…" />;

  const allItems = cartData?.items || [];
  const total = cartData?.total ?? 0;
  const currency = cartData?.currency || '';
  const hasTotal = total > 0;

  const itemsWithPrice = allItems.filter(it => it.unitPrice != null);
  const itemsNoPrice = allItems.filter(it => it.unitPrice == null);
  const totalPages = Math.max(1, Math.ceil(allItems.length / PAGE_SIZE));
  const pageItems = allItems.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  return (
    <div>
      <div className="page-header" style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
        <div>
          <h1 className="page-title">{activeTab === 'cart' ? 'My Order' : 'Order History'}</h1>
          {activeTab === 'cart' && (
            <p className="page-subtitle">
              {allItems.length} items
              {hasTotal ? ` · ${currency} ${Number(total).toFixed(2)}` : ''}
              {itemsNoPrice.length > 0 && ` · ${itemsNoPrice.length} without price`}
            </p>
          )}
        </div>
        <button className="btn btn-secondary" onClick={() => navigate('/catalog')}>
          ← Back to Catalog
        </button>
      </div>

      <div style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
        <button className={`btn ${activeTab === 'cart' ? 'btn-primary' : ''}`} onClick={() => setActiveTab('cart')}>
          Cart {allItems.length > 0 && `(${allItems.length})`}
        </button>
        <button className={`btn ${activeTab === 'orders' ? 'btn-primary' : ''}`} onClick={() => setActiveTab('orders')}>
          My Orders
        </button>
      </div>

      {activeTab === 'orders' && <OrdersSection />}

      {activeTab === 'cart' && (allItems.length === 0 ? (
        <div className="empty-state" style={{ marginTop: 40 }}>
          <div className="empty-state-icon">🛒</div>
          <h3>Your cart is empty</h3>
          <p>Go to the catalog and add products to your order.</p>
          <button className="btn btn-primary" style={{ marginTop: 16 }} onClick={() => navigate('/catalog')}>
            Browse Catalog
          </button>
        </div>
      ) : (
        <div style={{ display: 'flex', gap: 20, alignItems: 'flex-start', flexWrap: 'wrap' }}>

          {/* Main: item table */}
          <div style={{ flex: '1 1 600px', minWidth: 0 }}>
            <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
              <div style={{ overflowX: 'auto' }}>
                <table className="data-table" style={{ margin: 0 }}>
                  <thead>
                    <tr>
                      <th style={{ minWidth: 260 }}>Product / Price tiers</th>
                      <th style={{ textAlign: 'center', width: 140 }}>Qty</th>
                      <th style={{ textAlign: 'right', width: 90 }}>Unit</th>
                      <th style={{ textAlign: 'right', width: 90 }}>Total</th>
                      <th style={{ width: 40 }}></th>
                    </tr>
                  </thead>
                  <tbody>
                    {pageItems.map(item => (
                      <CartRow key={item.productId} item={item} onUpdate={handleCartUpdate} />
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Pagination */}
              {totalPages > 1 && (
                <div style={{
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  gap: 8, padding: '12px 16px', borderTop: '1px solid var(--wpw-border)',
                }}>
                  <button className="btn btn-secondary" style={{ fontSize: 12, padding: '4px 10px' }}
                    disabled={page <= 1} onClick={() => setPage(p => p - 1)}>← Prev</button>
                  <span style={{ fontSize: 13, color: 'var(--wpw-text-secondary)' }}>
                    {page} / {totalPages} ({allItems.length} items)
                  </span>
                  <button className="btn btn-secondary" style={{ fontSize: 12, padding: '4px 10px' }}
                    disabled={page >= totalPages} onClick={() => setPage(p => p + 1)}>Next →</button>
                </div>
              )}
            </div>
          </div>

          {/* Sidebar: summary + actions */}
          <div style={{ width: 280, flexShrink: 0 }}>

            {/* Summary */}
            <div className="card" style={{ marginBottom: 16 }}>
              <div className="card-title" style={{ marginBottom: 12 }}>Order Summary</div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: 8, fontSize: 14, marginBottom: 16 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--wpw-text-secondary)' }}>Total items</span>
                  <span style={{ fontWeight: 500 }}>{allItems.length}</span>
                </div>
                {itemsWithPrice.length > 0 && (
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span style={{ color: 'var(--wpw-text-secondary)' }}>Priced items</span>
                    <span>{itemsWithPrice.length}</span>
                  </div>
                )}
                {itemsNoPrice.length > 0 && (
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span style={{ color: 'var(--wpw-text-secondary)' }}>No price</span>
                    <span style={{ color: 'var(--wpw-error, #e53e3e)' }}>{itemsNoPrice.length}</span>
                  </div>
                )}
                <div style={{ borderTop: '1px solid var(--wpw-border)', paddingTop: 8, display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ fontWeight: 600 }}>Subtotal</span>
                  <span style={{ fontWeight: 700, fontSize: 16 }}>
                    {hasTotal ? `${currency} ${Number(total).toFixed(2)}` : '—'}
                  </span>
                </div>
              </div>

              <button
                className="btn btn-primary"
                style={{ width: '100%', marginBottom: 8 }}
                onClick={handleCheckout}
                disabled={checkingOut}
              >
                {checkingOut ? 'Processing…' : '📦 Submit Order'}
              </button>
              <button
                className="btn btn-secondary"
                style={{ width: '100%', fontSize: 13 }}
                onClick={() => exportCSV(allItems, currency, notes)}
              >
                ↓ Export CSV
              </button>
            </div>

            {/* Notes */}
            <div className="card">
              <div className="card-title" style={{ marginBottom: 8 }}>Order Notes</div>
              <textarea
                value={notes}
                onChange={e => setNotes(e.target.value)}
                placeholder="Delivery address, special instructions, reference number…"
                style={{
                  width: '100%', minHeight: 100, resize: 'vertical',
                  border: '1px solid var(--wpw-border)', borderRadius: 6,
                  padding: '8px 10px', fontSize: 13, fontFamily: 'inherit',
                  boxSizing: 'border-box',
                }}
              />
            </div>

            {/* Danger */}
            <div style={{ marginTop: 12 }}>
              <button
                className="btn btn-secondary"
                style={{ width: '100%', fontSize: 13, color: 'var(--wpw-error, #e53e3e)' }}
                onClick={handleClear}
              >
                🗑 Clear all items
              </button>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
