import { useNavigate } from 'react-router-dom';
import { useRef } from 'react';

const PLACEHOLDER_SVG = (
  <svg className="placeholder-svg" viewBox="0 0 80 80" fill="none" xmlns="http://www.w3.org/2000/svg">
    <rect width="80" height="80" rx="4" fill="#eceff1" />
    <path d="M20 56 L32 38 L44 50 L52 40 L62 56 H20Z" fill="#b0bec5" />
    <circle cx="52" cy="28" r="8" fill="#b0bec5" />
    <path d="M10 10 H70 V70 H10 Z" stroke="#b0bec5" strokeWidth="2" fill="none" />
  </svg>
);

const STOCK_LABELS = {
  in_stock: 'In Stock',
  low_stock: 'Low Stock',
  out_of_stock: 'Out of Stock',
};

function getActiveTier(tiers, qty) {
  if (!tiers || tiers.length === 0) return null;
  return tiers
    .filter(t => t.minQty <= qty)
    .sort((a, b) => b.minQty - a.minQty)[0] ?? tiers[0];
}

import { useState, useEffect } from 'react';

// Самодостаточный контрол — управляет строкой ввода сам,
// вызывает onChange только для подтверждённого числа
function QtyControl({ qty, onChange }) {
  const [display, setDisplay] = useState(String(qty));
  useEffect(() => { setDisplay(String(qty)); }, [qty]);

  return (
    <div className="product-qty-control" onClick={e => e.stopPropagation()} onMouseDown={e => e.stopPropagation()}>
      <button
        className="product-qty-btn"
        onClick={() => onChange(Math.max(0, qty - 1))}
        tabIndex={-1}
        disabled={qty === 0}
      >−</button>
      <input
        className="product-qty-input"
        type="text"
        inputMode="numeric"
        value={display}
        onChange={e => {
          const raw = e.target.value.replace(/[^0-9]/g, '');
          setDisplay(raw);
          const n = parseInt(raw, 10);
          if (!isNaN(n)) onChange(n);
        }}
        onFocus={e => e.target.select()}
        onBlur={() => {
          const n = parseInt(display, 10);
          if (!display || isNaN(n)) { onChange(0); setDisplay('0'); }
          else setDisplay(String(n));
        }}
        onClick={e => e.stopPropagation()}
      />
      <button
        className="product-qty-btn"
        onClick={() => onChange(qty + 1)}
        tabIndex={-1}
      >+</button>
    </div>
  );
}

// qty / onQtyChange — для дилеров, управляется из CatalogPage (единый источник истины)
export default function ProductCard({ product, qty, onQtyChange, inCart, showStock, onAddToCart, onDragStart, onDragMove }) {
  const navigate = useNavigate();
  const suppressClick = useRef(false);
  const [showTiers, setShowTiers] = useState(false);

  const imageUrl = product.thumbnailUrl || product.thumbnail_url || product.imageUrl || product.image_url || product.mainImageUrl;
  const name = product.name || product.productName || product.product_name || '';
  const toolNo = product.toolNo || product.tool_no || '';
  const dealerSku = product.dealerSku || product.dealer_sku || null;
  const stockKey = product.stockStatus || product.stock_status || 'out_of_stock';

  const tiers = product.price?.tiers ?? [];
  const symbol = product.price?.currencySymbol ?? '';
  const activeTier = getActiveTier(tiers, qty > 0 ? qty : 1);
  const hasPrice = tiers.length > 0;
  const isDealer = !!onAddToCart;
  const qtyVal = qty ?? 0;
  const isStaged = qtyVal > 0 && !inCart;
  const isStagedForRemoval = qtyVal > 0 && inCart;

  function handleNavigate() {
    navigate(`/product/${encodeURIComponent(toolNo || product.id)}`);
  }

  function handleBodyClick() {
    if (suppressClick.current) { suppressClick.current = false; return; }
    handleNavigate();
  }

  const cardStyle = isStagedForRemoval
    ? { outline: '2px solid #e53e3e', outlineOffset: 2 }
    : isStaged
      ? { outline: '2px solid var(--wpw-accent)', outlineOffset: 2 }
      : inCart
        ? { outline: '2px solid #43a047', outlineOffset: 2 }
        : undefined;

  return (
    <div
      className="product-card"
      style={cardStyle}
      onMouseDown={onDragStart ? e => {
        if (e.target.closest('input,button,.product-card-image,.product-card-dealer-footer')) return;
        e.preventDefault();
        suppressClick.current = true;
        onDragStart();
      } : undefined}
      onMouseEnter={onDragMove ? () => onDragMove() : undefined}
    >
      <div
        className="product-card-image"
        onClick={handleNavigate}
        style={{ cursor: 'pointer', position: 'relative' }}
      >
        {imageUrl ? (
          <img src={imageUrl} alt={name} loading="lazy"
            onError={e => { e.currentTarget.style.display = 'none'; e.currentTarget.nextSibling.style.display = 'flex'; }} />
        ) : null}
        <div style={{ display: imageUrl ? 'none' : 'flex', alignItems: 'center', justifyContent: 'center' }}>
          {PLACEHOLDER_SVG}
        </div>
        {inCart && (
          <div style={{
            position: 'absolute', top: 6, left: 6, background: '#43a047', color: '#fff',
            borderRadius: 4, padding: '2px 6px', fontSize: 11, fontWeight: 600, lineHeight: 1.4, pointerEvents: 'none',
          }}>In cart</div>
        )}
      </div>

      <div className="product-card-body" onClick={handleBodyClick} style={{ cursor: 'default' }}>
        <div className="product-card-toolno">
          {toolNo}
          {dealerSku && <span className="product-dealer-sku">Your SKU: {dealerSku}</span>}
        </div>
        <div className="product-card-name">{name}</div>

        {/* Dealer footer — полностью изолирован от drag-системы */}
        {isDealer && (
          <div
            className="product-card-dealer-footer"
            onClick={e => e.stopPropagation()}
            onMouseDown={e => e.stopPropagation()}
          >
            {inCart ? (
              <span style={{
                fontSize: 12, fontWeight: 600, color: '#43a047',
                background: '#e8f5e9', borderRadius: 4, padding: '3px 8px',
              }}>✓ In cart</span>
            ) : (
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
                <QtyControl qty={qty ?? 0} onChange={onQtyChange} />
                <button
                  className="btn btn-primary product-add-btn"
                  onClick={() => { if ((qty ?? 0) > 0) onAddToCart(product, qty); }}
                  disabled={(qty ?? 0) === 0}
                >+ Add</button>
              </div>
            )}

            {hasPrice && (
              <div
                className="product-price-row"
                onMouseEnter={() => setShowTiers(true)}
                onMouseLeave={() => setShowTiers(false)}
              >
                <span className="product-price-value">
                  {activeTier
                    ? `${symbol}${Number(activeTier.price).toFixed(2)}`
                    : `${symbol}${Number(tiers[0].price).toFixed(2)}`
                  }
                  <span className="product-price-unit">/unit</span>
                </span>
                {tiers.length > 1 && <span className="product-price-tiers-hint">tiers ▾</span>}
                {showTiers && tiers.length > 1 && (
                  <div className="product-price-tiers-popup">
                    {[...tiers].sort((a, b) => a.minQty - b.minQty).map(tier => {
                      const isActive = activeTier && tier.minQty === activeTier.minQty;
                      return (
                        <div key={tier.minQty} className={`product-tier-row${isActive ? ' active' : ''}`}>
                          <span className="tier-qty">{tier.minQty}+</span>
                          <span className="tier-price">{symbol}{Number(tier.price).toFixed(2)}</span>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            )}
          </div>
        )}

        {/* Admin: stock badge + price */}
        {!isDealer && showStock && (
          <div className="product-card-footer">
            <span className={`stock-badge ${stockKey}`}>{STOCK_LABELS[stockKey] || stockKey}</span>
            {hasPrice && (
              <span className="stock-badge price-active">
                {symbol}{Number(tiers[0].price).toFixed(2)}{tiers.length > 1 && ' +'}
              </span>
            )}
          </div>
        )}

        {/* Public: price only */}
        {!isDealer && !showStock && hasPrice && (
          <div className="product-card-footer">
            <span className="stock-badge price-active">
              {symbol}{Number(tiers[0].price).toFixed(2)}{tiers.length > 1 && ' +'}
            </span>
          </div>
        )}
      </div>
    </div>
  );
}
