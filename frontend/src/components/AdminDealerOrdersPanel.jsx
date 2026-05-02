import { useState, useEffect } from 'react';
import { getAdminDealerOrders, getAdminOrder, changeOrderStatus } from '../api/api';
import { useToast } from './ToastContext';

const STATUS_LABELS = {
  SUBMITTED:     { admin: 'New',          color: '#1565c0', bg: '#e3f2fd' },
  IN_PROCESSING: { admin: 'In Processing', color: '#e65100', bg: '#fff3e0' },
  CONFIRMED:     { admin: 'Confirmed',    color: '#2e7d32', bg: '#e8f5e9' },
  REJECTED:      { admin: 'Rejected',     color: '#c62828', bg: '#ffebee' },
};

const NEXT_STATUSES = {
  SUBMITTED:     ['IN_PROCESSING', 'CONFIRMED', 'REJECTED'],
  IN_PROCESSING: ['CONFIRMED', 'REJECTED'],
  CONFIRMED:     [],
  REJECTED:      [],
};

function StatusBadge({ status }) {
  const s = STATUS_LABELS[status] || { admin: status, color: '#555', bg: '#f5f5f5' };
  return (
    <span style={{
      display: 'inline-block', padding: '2px 8px', borderRadius: 4,
      fontSize: 12, fontWeight: 600, color: s.color, background: s.bg,
    }}>
      {s.admin}
    </span>
  );
}

function OrderDetailModal({ orderId, onClose, onStatusChanged }) {
  const { showToast } = useToast();
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [changing, setChanging] = useState(false);

  useEffect(() => {
    getAdminOrder(orderId)
      .then(setOrder)
      .catch(() => showToast('Failed to load order', 'error'))
      .finally(() => setLoading(false));
  }, [orderId]);

  async function handleStatus(newStatus) {
    setChanging(true);
    try {
      const updated = await changeOrderStatus(orderId, newStatus);
      setOrder(updated);
      onStatusChanged?.(updated);
      showToast('Status updated', 'success');
    } catch {
      showToast('Error changing status', 'error');
    } finally {
      setChanging(false);
    }
  }

  return (
    <div style={{
      position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
    }} onClick={onClose}>
      <div style={{
        background: '#fff', borderRadius: 8, padding: 24, maxWidth: 700, width: '100%',
        maxHeight: '85vh', overflowY: 'auto', boxShadow: '0 8px 32px rgba(0,0,0,0.18)',
      }} onClick={e => e.stopPropagation()}>
        {loading && <div style={{ padding: 40, textAlign: 'center', color: '#888' }}>Loading...</div>}

        {order && (
          <>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 20 }}>
              <div>
                <div style={{ fontSize: 16, fontWeight: 700, marginBottom: 4 }}>
                  Order #{order.id.slice(0, 8).toUpperCase()}
                </div>
                <div style={{ fontSize: 13, color: '#666' }}>
                  {new Date(order.submittedAt).toLocaleString('ru-RU')}
                </div>
              </div>
              <button onClick={onClose} style={{ background: 'none', border: 'none', fontSize: 20, cursor: 'pointer', color: '#999' }}>×</button>
            </div>

            <div style={{ display: 'flex', gap: 12, marginBottom: 20, flexWrap: 'wrap' }}>
              <div style={{ fontSize: 13 }}>Status: <StatusBadge status={order.status} /></div>
              <div style={{ fontSize: 13 }}>Total: <strong>{Number(order.total).toFixed(2)} {order.currency}</strong></div>
              <div style={{ fontSize: 13 }}>Items: <strong>{order.items.length}</strong></div>
            </div>

            {NEXT_STATUSES[order.status]?.length > 0 && (
              <div style={{ marginBottom: 20, display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                <span style={{ fontSize: 13, alignSelf: 'center', color: '#555' }}>Change status:</span>
                {NEXT_STATUSES[order.status].map(s => (
                  <button
                    key={s}
                    onClick={() => handleStatus(s)}
                    disabled={changing}
                    className="btn"
                    style={{
                      fontSize: 12, padding: '4px 12px',
                      background: STATUS_LABELS[s]?.bg,
                      color: STATUS_LABELS[s]?.color,
                      border: `1px solid ${STATUS_LABELS[s]?.color}`,
                    }}
                  >
                    {STATUS_LABELS[s]?.admin}
                  </button>
                ))}
              </div>
            )}

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
                    <td style={{ padding: '6px 8px', textAlign: 'right' }}>
                      {item.unitPrice != null ? `${Number(item.unitPrice).toFixed(2)}` : '—'}
                    </td>
                    <td style={{ padding: '6px 8px', textAlign: 'right', fontWeight: 600 }}>
                      {item.lineTotal != null ? `${Number(item.lineTotal).toFixed(2)}` : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr>
                  <td colSpan={4} style={{ padding: '8px 8px', textAlign: 'right', fontWeight: 700 }}>Total:</td>
                  <td style={{ padding: '8px 8px', textAlign: 'right', fontWeight: 700 }}>
                    {Number(order.total).toFixed(2)} {order.currency}
                  </td>
                </tr>
              </tfoot>
            </table>
          </>
        )}
      </div>
    </div>
  );
}

export default function AdminDealerOrdersPanel({ dealerId }) {
  const { showToast } = useToast();
  const [orders, setOrders] = useState(null);
  const [loading, setLoading] = useState(true);
  const [selectedOrderId, setSelectedOrderId] = useState(null);

  useEffect(() => {
    if (!dealerId) return;
    setLoading(true);
    getAdminDealerOrders(dealerId)
      .then(setOrders)
      .catch(() => showToast('Failed to load orders', 'error'))
      .finally(() => setLoading(false));
  }, [dealerId]);

  function handleStatusChanged(updatedOrder) {
    setOrders(prev => prev.map(o => o.id === updatedOrder.id
      ? { ...o, status: updatedOrder.status, statusLabel: updatedOrder.statusLabel, updatedAt: updatedOrder.updatedAt }
      : o
    ));
  }

  if (loading) return <div style={{ padding: 20, color: '#888', fontSize: 13 }}>Loading orders...</div>;

  if (!orders || orders.length === 0) {
    return <div style={{ padding: 20, color: '#888', fontSize: 13 }}>No orders</div>;
  }

  return (
    <div>
      <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
        <thead>
          <tr style={{ borderBottom: '2px solid var(--wpw-border)', background: '#f9fafb' }}>
            <th style={{ padding: '8px 10px', textAlign: 'left' }}>#</th>
            <th style={{ padding: '8px 10px', textAlign: 'left' }}>Date</th>
            <th style={{ padding: '8px 10px', textAlign: 'center' }}>Status</th>
            <th style={{ padding: '8px 10px', textAlign: 'right' }}>Items</th>
            <th style={{ padding: '8px 10px', textAlign: 'right' }}>Total</th>
            <th style={{ padding: '8px 10px' }}></th>
          </tr>
        </thead>
        <tbody>
          {orders.map(o => (
            <tr key={o.id} style={{ borderBottom: '1px solid var(--wpw-border)' }}>
              <td style={{ padding: '8px 10px', fontFamily: 'monospace', color: '#888' }}>
                {o.id.slice(0, 8).toUpperCase()}
              </td>
              <td style={{ padding: '8px 10px' }}>
                {new Date(o.submittedAt).toLocaleString('ru-RU')}
              </td>
              <td style={{ padding: '8px 10px', textAlign: 'center' }}>
                <StatusBadge status={o.status} />
              </td>
              <td style={{ padding: '8px 10px', textAlign: 'right' }}>{o.itemCount}</td>
              <td style={{ padding: '8px 10px', textAlign: 'right', fontWeight: 600 }}>
                {Number(o.total).toFixed(2)} {o.currency}
              </td>
              <td style={{ padding: '8px 10px' }}>
                <button
                  className="btn btn-secondary"
                  style={{ padding: '3px 10px', fontSize: 12 }}
                  onClick={() => setSelectedOrderId(o.id)}
                >
                  Open
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {selectedOrderId && (
        <OrderDetailModal
          orderId={selectedOrderId}
          onClose={() => setSelectedOrderId(null)}
          onStatusChanged={handleStatusChanged}
        />
      )}
    </div>
  );
}
