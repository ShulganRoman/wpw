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

export default function ProductCard({ product, selectable, selected, inCart, onSelect, onAddToCart, onDragStart, onDragMove }) {
  const navigate = useNavigate();
  const suppressClick = useRef(false);
  const stockKey = product.stockStatus || product.stock_status || 'out_of_stock';

  const imageUrl = product.thumbnailUrl || product.thumbnail_url || product.imageUrl || product.image_url || product.mainImageUrl;
  const name = product.name || product.productName || product.product_name || '';
  const toolNo = product.toolNo || product.tool_no || '';
  const dealerSku = product.dealerSku || product.dealer_sku || null;

  function handleNavigate() {
    navigate(`/product/${encodeURIComponent(toolNo || product.id)}`);
  }

  function handleBodyClick() {
    if (suppressClick.current) { suppressClick.current = false; return; }
    if (selectable) onSelect?.();
    else handleNavigate();
  }

  const cardStyle = selected
    ? { outline: '2px solid var(--wpw-accent)', outlineOffset: 2 }
    : inCart
      ? { outline: '2px solid #43a047', outlineOffset: 2 }
      : undefined;

  return (
    <div
      className="product-card"
      style={cardStyle}
      onMouseDown={onDragStart ? e => { if (e.target.closest('label,input,button,.product-card-image')) return; e.preventDefault(); suppressClick.current = true; onDragStart(!!selected); } : undefined}
      onMouseEnter={onDragMove ? () => onDragMove() : undefined}
    >
      <div
        className="product-card-image"
        onClick={handleNavigate}
        style={{ cursor: 'pointer', position: 'relative' }}
      >
        {imageUrl ? (
          <img
            src={imageUrl}
            alt={name}
            loading="lazy"
            onError={e => { e.currentTarget.style.display = 'none'; e.currentTarget.nextSibling.style.display = 'flex'; }}
          />
        ) : null}
        <div style={{ display: imageUrl ? 'none' : 'flex', alignItems: 'center', justifyContent: 'center' }}>
          {PLACEHOLDER_SVG}
        </div>
        {inCart && (
          <div style={{
            position: 'absolute', top: 6, left: 6,
            background: '#43a047', color: '#fff',
            borderRadius: 4, padding: '2px 6px',
            fontSize: 11, fontWeight: 600, lineHeight: 1.4,
            pointerEvents: 'none',
          }}>
            In cart
          </div>
        )}
        {onAddToCart && !inCart && (
          <button
            onClick={e => { e.stopPropagation(); onAddToCart(product); }}
            title="Add to cart"
            style={{
              position: 'absolute', bottom: 6, right: 6,
              background: 'var(--wpw-accent)', color: '#fff',
              border: 'none', borderRadius: 6,
              padding: '4px 8px', fontSize: 12, cursor: 'pointer',
            }}
          >
            + Cart
          </button>
        )}
      </div>
      <div
        className="product-card-body"
        onClick={handleBodyClick}
        style={{ cursor: selectable ? 'pointer' : 'default' }}
      >
        {selectable && (
          <label
            style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 4, cursor: 'pointer' }}
            onClick={e => e.stopPropagation()}
          >
            <input
              type="checkbox"
              checked={!!selected}
              onChange={e => { e.stopPropagation(); onSelect?.(); }}
              style={{ width: 15, height: 15, cursor: 'pointer', flexShrink: 0 }}
            />
            <span style={{ fontSize: 11, color: 'var(--wpw-text-secondary)' }}>
              {selected ? 'Selected' : 'Select'}
            </span>
          </label>
        )}
        <div className="product-card-toolno">
          {toolNo}
          {dealerSku && <span className="product-dealer-sku">Your SKU: {dealerSku}</span>}
        </div>
        <div className="product-card-name">{name}</div>
        <div className="product-card-footer">
          <span className={`stock-badge ${stockKey}`}>
            {STOCK_LABELS[stockKey] || stockKey}
          </span>
          {product.price?.tiers?.length > 0 && (
            <span className={`stock-badge ${product.price.expired ? 'price-expired' : 'price-active'}`}>
              {product.price.currencySymbol}{Number(product.price.tiers[0].price).toFixed(2)}
              {product.price.tiers.length > 1 && ' +'}
            </span>
          )}
        </div>
      </div>
    </div>
  );
}
