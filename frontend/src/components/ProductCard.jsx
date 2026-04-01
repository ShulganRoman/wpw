import { useNavigate } from 'react-router-dom';

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

export default function ProductCard({ product }) {
  const navigate = useNavigate();
  const stockKey = product.stockStatus || product.stock_status || 'out_of_stock';

  function handleClick() {
    navigate(`/product/${encodeURIComponent(product.toolNo || product.tool_no || product.id)}`);
  }

  const imageUrl = product.thumbnailUrl || product.thumbnail_url || product.imageUrl || product.image_url || product.mainImageUrl;
  const name = product.name || product.productName || product.product_name || '';
  const toolNo = product.toolNo || product.tool_no || '';

  return (
    <div className="product-card" onClick={handleClick} role="button" tabIndex={0}
      onKeyDown={e => e.key === 'Enter' && handleClick()}
    >
      <div className="product-card-image">
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
      </div>
      <div className="product-card-body">
        <div className="product-card-toolno">{toolNo}</div>
        <div className="product-card-name">{name}</div>
        <div className="product-card-footer">
          <span className={`stock-badge ${stockKey}`}>
            {STOCK_LABELS[stockKey] || stockKey}
          </span>
        </div>
      </div>
    </div>
  );
}
