import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { updateCartQty, removeFromCart, clearCart, getCart } from '../api/api';
import { useToast } from './ToastContext';

function QtyControl({ qty, onDecrease, onIncrease, onChange }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
      <button
        onClick={onDecrease}
        style={{
          width: 24, height: 24, border: '1px solid var(--wpw-border)', borderRadius: 4,
          background: 'var(--wpw-surface)', cursor: 'pointer', fontSize: 14, lineHeight: 1,
        }}
      >−</button>
      <input
        type="number" min="1" value={qty}
        onChange={e => onChange(parseInt(e.target.value, 10) || 1)}
        style={{
          width: 44, textAlign: 'center', border: '1px solid var(--wpw-border)',
          borderRadius: 4, padding: '2px 4px', fontSize: 13,
        }}
      />
      <button
        onClick={onIncrease}
        style={{
          width: 24, height: 24, border: '1px solid var(--wpw-border)', borderRadius: 4,
          background: 'var(--wpw-surface)', cursor: 'pointer', fontSize: 14, lineHeight: 1,
        }}
      >+</button>
    </div>
  );
}

function CartItemRow({ item, onUpdate }) {
  const toast = useToast();
  const [qty, setQty] = useState(item.qty);
  const [updating, setUpdating] = useState(false);

  useEffect(() => { setQty(item.qty); }, [item.qty]);

  async function applyQty(newQty) {
    if (newQty < 1) return;
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

  const nextTier = item.tiers ? item.tiers.find(t => t.minQty > qty) : null;

  return (
    <div style={{
      display: 'flex', gap: 10, padding: '10px 0',
      borderBottom: '1px solid var(--wpw-border)',
      opacity: updating ? 0.6 : 1, transition: 'opacity 0.15s',
    }}>
      {item.imageUrl && (
        <img src={item.imageUrl} alt={item.toolNo}
          style={{
            width: 44, height: 44, objectFit: 'contain', borderRadius: 4,
            border: '1px solid var(--wpw-border)', flexShrink: 0,
          }} />
      )}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 11, fontFamily: 'var(--wpw-font-mono)', color: 'var(--wpw-text-secondary)' }}>
          {item.toolNo}
        </div>
        <div style={{
          fontSize: 13, fontWeight: 500, overflow: 'hidden',
          textOverflow: 'ellipsis', whiteSpace: 'nowrap', marginBottom: 5,
        }}>
          {item.name}
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <QtyControl
            qty={qty}
            onDecrease={() => { const n = qty - 1; setQty(n); applyQty(n); }}
            onIncrease={() => { const n = qty + 1; setQty(n); applyQty(n); }}
            onChange={n => { setQty(n); applyQty(n); }}
          />
          <button
            onClick={handleRemove}
            style={{
              background: 'none', border: 'none',
              color: 'var(--wpw-error, #e53e3e)',
              cursor: 'pointer', fontSize: 16, padding: '0 2px', lineHeight: 1,
            }}
          >✕</button>
        </div>
        {nextTier && (
          <div style={{ fontSize: 11, color: 'var(--wpw-accent)', marginTop: 3 }}>
            Buy {nextTier.minQty}+ → {nextTier.price}
          </div>
        )}
      </div>
      <div style={{ textAlign: 'right', flexShrink: 0, minWidth: 60 }}>
        {item.unitPrice != null ? (
          <>
            <div style={{ fontSize: 13, fontWeight: 600 }}>{Number(item.lineTotal).toFixed(2)}</div>
            <div style={{ fontSize: 11, color: 'var(--wpw-text-secondary)' }}>
              {Number(item.unitPrice).toFixed(2)} × {qty}
            </div>
          </>
        ) : (
          <div style={{ fontSize: 11, color: 'var(--wpw-text-secondary)' }}>No price</div>
        )}
      </div>
    </div>
  );
}

export default function CartSidebar({ open, onClose, cartData, onCartUpdate }) {
  const toast = useToast();
  const navigate = useNavigate();

  useEffect(() => {
    if (cartData?.removedToolNos?.length > 0) {
      toast(`Removed from cart (deactivated): ${cartData.removedToolNos.join(', ')}`, 'warning');
    }
  }, [cartData?.removedToolNos?.join(',')]);

  async function handleClear() {
    try {
      await clearCart();
      const fresh = await getCart();
      onCartUpdate(fresh);
    } catch (err) {
      toast(err.message, 'error');
    }
  }

  const items = cartData?.items || [];
  const total = cartData?.total ?? 0;
  const currency = cartData?.currency || '';
  const hasTotal = total > 0;

  return (
    <>
      {open && (
        <div
          onClick={onClose}
          style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.25)', zIndex: 999 }}
        />
      )}
      <div style={{
        position: 'fixed', top: 0, right: 0, height: '100vh', width: 360,
        background: 'var(--wpw-surface, #fff)',
        borderLeft: '1px solid var(--wpw-border)',
        boxShadow: '-4px 0 24px rgba(0,0,0,0.13)',
        transform: open ? 'translateX(0)' : 'translateX(100%)',
        transition: 'transform 0.25s ease',
        zIndex: 1000,
        display: 'flex', flexDirection: 'column',
      }}>
        {/* Header */}
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          padding: '16px 20px', borderBottom: '1px solid var(--wpw-border)', flexShrink: 0,
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{ fontSize: 18 }}>🛒</span>
            <span style={{ fontWeight: 600, fontSize: 16 }}>Cart</span>
            {items.length > 0 && (
              <span style={{
                background: 'var(--wpw-accent)', color: '#fff',
                borderRadius: 10, fontSize: 11, padding: '1px 7px', fontWeight: 600,
              }}>{items.length}</span>
            )}
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            {items.length > 0 && (
              <button onClick={handleClear} style={{
                background: 'none', border: 'none', cursor: 'pointer',
                fontSize: 12, color: 'var(--wpw-text-secondary)',
                textDecoration: 'underline', padding: 0,
              }}>Clear all</button>
            )}
            <button onClick={onClose} style={{
              background: 'none', border: 'none', cursor: 'pointer',
              fontSize: 22, color: 'var(--wpw-text-secondary)', lineHeight: 1,
            }}>✕</button>
          </div>
        </div>

        {/* Items */}
        <div style={{ flex: 1, overflowY: 'auto', padding: '0 20px' }}>
          {items.length === 0 ? (
            <div style={{ textAlign: 'center', color: 'var(--wpw-text-secondary)', marginTop: 60 }}>
              <div style={{ fontSize: 36, marginBottom: 12 }}>🛒</div>
              <div style={{ fontWeight: 500, marginBottom: 6 }}>Cart is empty</div>
              <div style={{ fontSize: 13 }}>Select products in the catalog to add them</div>
            </div>
          ) : (
            items.map(item => (
              <CartItemRow key={item.productId} item={item} onUpdate={onCartUpdate} />
            ))
          )}
        </div>

        {/* Footer */}
        {items.length > 0 && (
          <div style={{
            padding: '16px 20px', borderTop: '1px solid var(--wpw-border)',
            flexShrink: 0, background: 'var(--wpw-surface)',
          }}>
            <div style={{
              display: 'flex', justifyContent: 'space-between',
              marginBottom: 12, fontSize: 15,
            }}>
              <span style={{ fontWeight: 500 }}>Total ({items.length} items)</span>
              <span style={{ fontWeight: 700 }}>
                {hasTotal ? `${currency} ${Number(total).toFixed(2)}` : '—'}
              </span>
            </div>
            <button
              className="btn btn-primary"
              style={{ width: '100%' }}
              onClick={() => { onClose(); navigate('/dealer'); }}
            >
              View order →
            </button>
          </div>
        )}
      </div>
    </>
  );
}
