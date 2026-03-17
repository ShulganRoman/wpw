import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getProduct, getSpareParts, getCompatibleTools } from '../api/api';
import ProductCard from '../components/ProductCard';
import { LoadingSpinner, ErrorState } from '../components/LoadingState';
import { useToast } from '../components/ToastContext';

const PLACEHOLDER_SVG = (
  <svg viewBox="0 0 160 160" width="140" height="140" fill="none" xmlns="http://www.w3.org/2000/svg">
    <rect width="160" height="160" rx="8" fill="#eceff1" />
    <path d="M40 112 L64 76 L88 100 L104 80 L124 112 H40Z" fill="#b0bec5" />
    <circle cx="104" cy="56" r="16" fill="#b0bec5" />
  </svg>
);

const STOCK_LABELS = {
  in_stock: 'In Stock',
  low_stock: 'Low Stock',
  out_of_stock: 'Out of Stock',
};

function Gallery({ images }) {
  const [active, setActive] = useState(0);

  if (!images || images.length === 0) {
    return (
      <div className="product-gallery">
        <div className="gallery-main">{PLACEHOLDER_SVG}</div>
      </div>
    );
  }

  return (
    <div className="product-gallery">
      <div className="gallery-main">
        <img src={images[active]} alt="Product" />
      </div>
      {images.length > 1 && (
        <div className="gallery-thumbs">
          {images.map((img, i) => (
            <img
              key={i}
              className={`gallery-thumb${i === active ? ' active' : ''}`}
              src={img}
              alt={`View ${i + 1}`}
              onClick={() => setActive(i)}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function formatValue(val) {
  if (val === null || val === undefined) return null;
  if (typeof val === 'boolean') return val ? 'Yes' : 'No';
  if (Array.isArray(val)) return val.length > 0 ? val.join(', ') : null;
  if (typeof val === 'object' && val instanceof Set) return [...val].join(', ');
  if (val === '' || val === 0) return null;
  return String(val);
}

function SpecsTable({ product }) {
  const attrs = product.attributes || {};
  const rows = [];

  function add(label, val) {
    const formatted = formatValue(val);
    if (formatted !== null) rows.push({ label, value: formatted });
  }

  add('Tool No', product.toolNo);
  if (product.altToolNo) add('Alt Tool No', product.altToolNo);
  add('Category', product.categoryName);
  add('Group', product.groupName);
  add('Product Type', product.productType);
  add('Catalog Page', product.catalogPage);

  add('D (mm)', attrs.dMm);
  add('D1 (mm)', attrs.d1Mm);
  add('D2 (mm)', attrs.d2Mm);
  add('B / Cutting Length (mm)', attrs.bMm);
  add('B1 (mm)', attrs.b1Mm);
  add('L / Total (mm)', attrs.lMm);
  add('L1 (mm)', attrs.l1Mm);
  add('R (mm)', attrs.rMm);
  add('A (mm)', attrs.aMm);
  add('Angle (°)', attrs.angleDeg);
  add('Shank (mm)', attrs.shankMm);
  add('Shank (inch)', attrs.shankInch);
  add('Flutes', attrs.flutes);
  add('Blade No', attrs.bladeNo);
  add('Cutting Type', attrs.cuttingType);
  add('Rotation Direction', attrs.rotationDirection);
  add('Bore Type', attrs.boreType);
  if (attrs.ballBearingCode) {
    add('Ball Bearing', attrs.ballBearingCode);
  } else {
    add('Ball Bearing', attrs.hasBallBearing);
  }
  if (attrs.retainerCode) {
    add('Retainer', attrs.retainerCode);
  } else {
    add('Retainer', attrs.hasRetainer);
  }
  add('Can Resharpen', attrs.canResharpen);

  add('Tool Materials', product.toolMaterials);
  add('Workpiece Materials', product.workpieceMaterials);
  add('Machine Types', product.machineTypes);
  add('Machine Brands', product.machineBrands);
  add('Operations', product.operationCodes);

  add('EAN-13', attrs.ean13);
  add('UPC-12', attrs.upc12);
  add('HS Code', attrs.hsCode);
  add('Country of Origin', attrs.countryOfOrigin);
  add('Weight (g)', attrs.weightG);
  add('Package Qty', attrs.pkgQty);
  add('Carton Qty', attrs.cartonQty);
  add('Stock Status', attrs.stockStatus);
  add('Stock Qty', attrs.stockQty);

  if (product.applications) add('Applications', product.applications);

  if (rows.length === 0) return <p style={{ color: 'var(--wpw-mid-gray)', fontSize: 13 }}>No specifications available.</p>;

  return (
    <table className="specs-table">
      <tbody>
        {rows.map((r, i) => (
          <tr key={i}>
            <td>{r.label}</td>
            <td>{r.value}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export default function ProductPage({ locale }) {
  const { toolNo } = useParams();
  const navigate = useNavigate();
  const toast = useToast();

  const [product, setProduct] = useState(null);
  const [spareParts, setSpareParts] = useState([]);
  const [compatibleTools, setCompatibleTools] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    async function fetchProduct() {
      setLoading(true);
      setError(null);
      try {
        const data = await getProduct(toolNo, locale);
        setProduct(data);
        const id = data.id || data.toolNo || data.tool_no;
        if (id) {
          getSpareParts(id, locale)
            .then(sp => setSpareParts(Array.isArray(sp) ? sp : sp?.items || []))
            .catch(() => {});
          getCompatibleTools(id, locale)
            .then(ct => setCompatibleTools(Array.isArray(ct) ? ct : ct?.items || []))
            .catch(() => {});
        }
      } catch (err) {
        setError(err.message);
        toast(err.message, 'error');
      } finally {
        setLoading(false);
      }
    }
    fetchProduct();
  }, [toolNo, locale]);

  if (loading) return <LoadingSpinner text="Loading product..." />;
  if (error) return <ErrorState message={error} onRetry={() => window.location.reload()} />;
  if (!product) return null;

  const name = product.name || product.toolNo || toolNo;
  const toolNoDisplay = product.toolNo || toolNo;
  const description = product.shortDescription || product.longDescription || '';
  const stockKey = product.attributes?.stockStatus || 'out_of_stock';
  const images = product.mediaUrls || [];

  return (
    <div className="product-detail">
      <button className="back-link" onClick={() => navigate(-1)}>
        &larr; Back
      </button>

      <div className="product-detail-top">
        <Gallery images={images} />

        <div className="product-info">
          <div className="product-toolno">{toolNoDisplay}</div>
          <h1 className="product-name">{name}</h1>

          <div className="product-meta">
            <span className={`stock-badge ${stockKey}`}>
              {STOCK_LABELS[stockKey] || stockKey}
            </span>
            {product.isOrderable && (
              <span className="orderable-badge">Orderable</span>
            )}
          </div>

          {description && (
            <p className="product-description">{description}</p>
          )}

          {product.sectionName && (
            <div className="product-breadcrumb">
              {[product.sectionName, product.categoryName, product.groupName].filter(Boolean).join(' > ')}
            </div>
          )}
        </div>
      </div>

      <div className="card">
        <div className="card-title">Specifications</div>
        <SpecsTable product={product} />
      </div>

      {spareParts.length > 0 && (
        <div className="related-section">
          <div className="related-section-title">Spare Parts</div>
          <div className="product-grid">
            {spareParts.map(p => (
              <ProductCard key={p.id || p.toolNo || p.tool_no} product={p} />
            ))}
          </div>
        </div>
      )}

      {compatibleTools.length > 0 && (
        <div className="related-section">
          <div className="related-section-title">Compatible Tools</div>
          <div className="product-grid">
            {compatibleTools.map(p => (
              <ProductCard key={p.id || p.toolNo || p.tool_no} product={p} />
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
