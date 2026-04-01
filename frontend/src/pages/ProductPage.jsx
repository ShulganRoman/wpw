import { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  getProduct,
  getSpareParts,
  getCompatibleTools,
  updateProduct,
  getProductImages,
  uploadProductImages,
  deleteProductImage,
  deleteProduct,
} from '../api/api';
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

function Lightbox({ images, initialIndex, onClose }) {
  const [current, setCurrent] = useState(initialIndex);
  const count = images.length;

  const goPrev = useCallback(() => {
    setCurrent(i => (i - 1 + count) % count);
  }, [count]);

  const goNext = useCallback(() => {
    setCurrent(i => (i + 1) % count);
  }, [count]);

  useEffect(() => {
    function handleKey(e) {
      if (e.key === 'Escape') onClose();
      if (e.key === 'ArrowLeft') goPrev();
      if (e.key === 'ArrowRight') goNext();
    }
    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, [onClose, goPrev, goNext]);

  useEffect(() => {
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => { document.body.style.overflow = prev; };
  }, []);

  return (
    <div
      className="lightbox-overlay"
      role="dialog"
      aria-modal="true"
      aria-label="Image viewer"
      onClick={onClose}
    >
      <button
        className="lightbox-close"
        aria-label="Close image viewer"
        onClick={onClose}
      >
        &#x2715;
      </button>

      <div
        className="lightbox-content"
        onClick={e => e.stopPropagation()}
      >
        {count > 1 && (
          <button
            className="lightbox-arrow lightbox-arrow--prev"
            aria-label="Previous image"
            onClick={goPrev}
          >
            &#8249;
          </button>
        )}

        <img
          className="lightbox-img"
          src={images[current]}
          alt={`Product view ${current + 1} of ${count}`}
        />

        {count > 1 && (
          <button
            className="lightbox-arrow lightbox-arrow--next"
            aria-label="Next image"
            onClick={goNext}
          >
            &#8250;
          </button>
        )}
      </div>

      {count > 1 && (
        <div
          className="lightbox-thumbs"
          onClick={e => e.stopPropagation()}
        >
          {images.map((img, i) => (
            <img
              key={i}
              className={`lightbox-thumb${i === current ? ' active' : ''}`}
              src={img}
              alt={`View ${i + 1}`}
              onClick={() => setCurrent(i)}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function Gallery({ images, editing, productId, imageData, onUpload, onDeleteImage }) {
  const [active, setActive] = useState(0);
  const [lightboxOpen, setLightboxOpen] = useState(false);
  const fileInputRef = useRef(null);

  // Keep active index in bounds when images change
  useEffect(() => {
    if (active >= images.length && images.length > 0) {
      setActive(images.length - 1);
    }
  }, [images.length, active]);

  function handleFileChange(e) {
    const files = Array.from(e.target.files);
    if (files.length === 0) return;
    onUpload(files);
    e.target.value = '';
  }

  if (!editing && (!images || images.length === 0)) {
    return (
      <div className="product-gallery">
        <div className="gallery-main">{PLACEHOLDER_SVG}</div>
      </div>
    );
  }

  return (
    <>
      <div className="product-gallery">
        <div className="gallery-main">
          {images.length > 0 ? (
            <img
              src={images[active]}
              alt="Product"
              className={editing ? undefined : 'gallery-main-img--clickable'}
              onClick={editing ? undefined : () => setLightboxOpen(true)}
            />
          ) : (
            PLACEHOLDER_SVG
          )}
        </div>

        {(images.length > 1 || editing) && (
          <div className="gallery-thumbs">
            {images.map((img, i) => {
              const imgEntry = imageData[i];
              return (
                <div key={imgEntry?.id ?? i} className="gallery-thumb-wrapper">
                  <img
                    className={`gallery-thumb${i === active ? ' active' : ''}`}
                    src={img}
                    alt={`View ${i + 1}`}
                    onClick={() => setActive(i)}
                  />
                  {editing && imgEntry && (
                    <button
                      className="gallery-thumb-delete"
                      aria-label="Delete image"
                      onClick={() => onDeleteImage(imgEntry.id)}
                    >
                      &times;
                    </button>
                  )}
                </div>
              );
            })}

            {editing && (
              <>
                <button
                  className="gallery-add-btn"
                  aria-label="Upload images"
                  onClick={() => fileInputRef.current?.click()}
                  type="button"
                >
                  +
                </button>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/*"
                  multiple
                  style={{ display: 'none' }}
                  onChange={handleFileChange}
                />
              </>
            )}
          </div>
        )}
      </div>

      {!editing && lightboxOpen && images.length > 0 && (
        <Lightbox
          images={images}
          initialIndex={active}
          onClose={() => setLightboxOpen(false)}
        />
      )}
    </>
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

function EditableSpecsTable({ product, editData, onChange }) {
  const attrs = product.attributes || {};
  const editAttrs = editData.attributes || {};

  function setAttr(key, value) {
    onChange('attributes', { ...editData.attributes, [key]: value });
  }

  function setField(key, value) {
    onChange(key, value);
  }

  function CollectionField({ label, fieldKey }) {
    const arr = editData[fieldKey] || [];
    return (
      <tr>
        <td>{label}</td>
        <td>
          <input
            className="edit-input"
            type="text"
            value={arr.join(', ')}
            onChange={e =>
              setField(
                fieldKey,
                e.target.value
                  .split(',')
                  .map(s => s.trim())
                  .filter(Boolean),
              )
            }
          />
        </td>
      </tr>
    );
  }

  function NumField({ label, attrKey }) {
    return (
      <tr>
        <td>{label}</td>
        <td>
          <input
            className="edit-input"
            type="number"
            step="any"
            value={editAttrs[attrKey] ?? ''}
            onChange={e =>
              setAttr(attrKey, e.target.value === '' ? null : Number(e.target.value))
            }
          />
        </td>
      </tr>
    );
  }

  function TextField({ label, attrKey }) {
    return (
      <tr>
        <td>{label}</td>
        <td>
          <input
            className="edit-input"
            type="text"
            value={editAttrs[attrKey] ?? ''}
            onChange={e => setAttr(attrKey, e.target.value)}
          />
        </td>
      </tr>
    );
  }

  function BoolField({ label, attrKey }) {
    return (
      <tr>
        <td>{label}</td>
        <td>
          <input
            type="checkbox"
            checked={!!editAttrs[attrKey]}
            onChange={e => setAttr(attrKey, e.target.checked)}
          />
        </td>
      </tr>
    );
  }

  return (
    <table className="specs-table">
      <tbody>
        <tr>
          <td>Tool No</td>
          <td style={{ color: 'var(--wpw-mid-gray)', fontStyle: 'italic' }}>{product.toolNo} (readonly)</td>
        </tr>
        <tr>
          <td>Alt Tool No</td>
          <td>
            <input
              className="edit-input"
              type="text"
              value={editData.altToolNo}
              onChange={e => setField('altToolNo', e.target.value)}
            />
          </td>
        </tr>
        <tr>
          <td>Category</td>
          <td style={{ color: 'var(--wpw-mid-gray)', fontStyle: 'italic' }}>{product.categoryName} (readonly)</td>
        </tr>
        <tr>
          <td>Group</td>
          <td style={{ color: 'var(--wpw-mid-gray)', fontStyle: 'italic' }}>{product.groupName} (readonly)</td>
        </tr>
        <tr>
          <td>Product Type</td>
          <td>
            <select
              className="edit-select"
              value={editData.productType}
              onChange={e => setField('productType', e.target.value)}
            >
              <option value="main">main</option>
              <option value="spare">spare</option>
              <option value="accessory">accessory</option>
            </select>
          </td>
        </tr>
        <tr>
          <td>Catalog Page</td>
          <td>
            <input
              className="edit-input"
              type="number"
              step="1"
              value={editData.catalogPage}
              onChange={e => setField('catalogPage', e.target.value)}
            />
          </td>
        </tr>

        <NumField label="D (mm)" attrKey="dMm" />
        <NumField label="D1 (mm)" attrKey="d1Mm" />
        <NumField label="D2 (mm)" attrKey="d2Mm" />
        <NumField label="B / Cutting Length (mm)" attrKey="bMm" />
        <NumField label="B1 (mm)" attrKey="b1Mm" />
        <NumField label="L / Total (mm)" attrKey="lMm" />
        <NumField label="L1 (mm)" attrKey="l1Mm" />
        <NumField label="R (mm)" attrKey="rMm" />
        <NumField label="A (mm)" attrKey="aMm" />
        <NumField label="Angle (°)" attrKey="angleDeg" />
        <NumField label="Shank (mm)" attrKey="shankMm" />
        <TextField label="Shank (inch)" attrKey="shankInch" />
        <NumField label="Flutes" attrKey="flutes" />
        <NumField label="Blade No" attrKey="bladeNo" />
        <TextField label="Cutting Type" attrKey="cuttingType" />
        <TextField label="Rotation Direction" attrKey="rotationDirection" />
        <TextField label="Bore Type" attrKey="boreType" />
        <TextField label="Ball Bearing Code" attrKey="ballBearingCode" />
        <BoolField label="Has Ball Bearing" attrKey="hasBallBearing" />
        <TextField label="Retainer Code" attrKey="retainerCode" />
        <BoolField label="Has Retainer" attrKey="hasRetainer" />
        <BoolField label="Can Resharpen" attrKey="canResharpen" />

        <CollectionField label="Tool Materials" fieldKey="toolMaterials" />
        <CollectionField label="Workpiece Materials" fieldKey="workpieceMaterials" />
        <CollectionField label="Machine Types" fieldKey="machineTypes" />
        <CollectionField label="Machine Brands" fieldKey="machineBrands" />
        <CollectionField label="Operations" fieldKey="operationCodes" />

        <TextField label="EAN-13" attrKey="ean13" />
        <TextField label="UPC-12" attrKey="upc12" />
        <TextField label="HS Code" attrKey="hsCode" />
        <TextField label="Country of Origin" attrKey="countryOfOrigin" />
        <NumField label="Weight (g)" attrKey="weightG" />
        <NumField label="Package Qty" attrKey="pkgQty" />
        <NumField label="Carton Qty" attrKey="cartonQty" />

        <tr>
          <td>Stock Status</td>
          <td>
            <select
              className="edit-select"
              value={editAttrs.stockStatus ?? ''}
              onChange={e => setAttr('stockStatus', e.target.value)}
            >
              <option value="">—</option>
              <option value="in_stock">In Stock</option>
              <option value="low_stock">Low Stock</option>
              <option value="out_of_stock">Out of Stock</option>
            </select>
          </td>
        </tr>

        <NumField label="Stock Qty" attrKey="stockQty" />
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

  // Edit mode state
  const [editing, setEditing] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [editData, setEditData] = useState(null);
  const [saving, setSaving] = useState(false);
  const [imageData, setImageData] = useState([]);

  const canEdit = (() => {
    try {
      const privs = JSON.parse(localStorage.getItem('userPrivileges') || '[]');
      return privs.includes('MODIFY_PRODUCTS');
    } catch { return false; }
  })();

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

  // Load image data when entering edit mode
  useEffect(() => {
    if (editing && product?.id) {
      getProductImages(product.id)
        .then(setImageData)
        .catch(() => {});
    }
  }, [editing, product?.id]);

  function startEditing() {
    setEditData({
      altToolNo: product.altToolNo || '',
      productType: product.productType || 'main',
      status: product.status || 'active',
      isOrderable: product.isOrderable ?? true,
      catalogPage: product.catalogPage || '',
      name: product.name || '',
      shortDescription: product.shortDescription || '',
      longDescription: product.longDescription || '',
      attributes: { ...(product.attributes || {}) },
      toolMaterials: [...(product.toolMaterials || [])],
      workpieceMaterials: [...(product.workpieceMaterials || [])],
      machineTypes: [...(product.machineTypes || [])],
      machineBrands: [...(product.machineBrands || [])],
      operationCodes: [...(product.operationCodes || [])],
    });
    setEditing(true);
  }

  function cancelEditing() {
    setEditing(false);
    setEditData(null);
    setImageData([]);
  }

  function handleEditChange(key, value) {
    setEditData(prev => ({ ...prev, [key]: value }));
  }

  async function handleSave() {
    setSaving(true);
    try {
      const dto = {
        ...editData,
        catalogPage: editData.catalogPage ? Number(editData.catalogPage) : null,
      };
      await updateProduct(product.id, locale || 'en', dto);
      const updated = await getProduct(toolNo, locale);
      setProduct(updated);
      setEditing(false);
      setEditData(null);
      setImageData([]);
      toast('Product updated', 'success');
    } catch (err) {
      toast(err.message, 'error');
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete() {
    if (!window.confirm(`Delete product "${product.toolNo}"? This action cannot be undone.`)) return;
    setDeleting(true);
    try {
      await deleteProduct(product.id);
      toast('Product deleted', 'success');
      navigate(-1);
    } catch (err) {
      toast(err.message, 'error');
    } finally {
      setDeleting(false);
    }
  }

  async function handleUpload(files) {
    try {
      const updated = await uploadProductImages(product.id, files);
      setImageData(updated);
      setProduct(prev => ({ ...prev, mediaUrls: updated.map(i => i.url) }));
      toast('Images uploaded', 'success');
    } catch (err) {
      toast(err.message, 'error');
    }
  }

  async function handleDeleteImage(imageId) {
    if (!confirm('Delete this image?')) return;
    try {
      const updated = await deleteProductImage(product.id, imageId);
      setImageData(updated);
      setProduct(prev => ({ ...prev, mediaUrls: updated.map(i => i.url) }));
      toast('Image deleted', 'success');
    } catch (err) {
      toast(err.message, 'error');
    }
  }

  if (loading) return <LoadingSpinner text="Loading product..." />;
  if (error) return <ErrorState message={error} onRetry={() => window.location.reload()} />;
  if (!product) return null;

  const name = editing ? editData.name : (product.name || product.toolNo || toolNo);
  const toolNoDisplay = product.toolNo || toolNo;
  const description = editing ? editData.shortDescription : (product.shortDescription || product.longDescription || '');
  const stockKey = product.attributes?.stockStatus || 'out_of_stock';
  const images = product.mediaUrls || [];

  return (
    <div className="product-detail">
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
        <button className="back-link" style={{ marginBottom: 0 }} onClick={() => navigate(-1)}>
          &larr; Back
        </button>

        {canEdit && (
          <div className="product-edit-actions">
            {editing ? (
              <>
                <button
                  className="btn-save"
                  onClick={handleSave}
                  disabled={saving}
                >
                  {saving ? 'Saving...' : 'Save'}
                </button>
                <button className="btn-cancel" onClick={cancelEditing} disabled={saving}>
                  Cancel
                </button>
              </>
            ) : (
              <>
                <button className="btn-edit" onClick={startEditing}>
                  Edit
                </button>
                <button className="btn-delete" onClick={handleDelete} disabled={deleting}>
                  {deleting ? 'Deleting...' : 'Delete'}
                </button>
              </>
            )}
          </div>
        )}
      </div>

      <div className="product-detail-top">
        <Gallery
          images={images}
          editing={editing}
          productId={product.id}
          imageData={imageData}
          onUpload={handleUpload}
          onDeleteImage={handleDeleteImage}
        />

        <div className="product-info">
          <div className="product-toolno">{toolNoDisplay}</div>

          {editing ? (
            <div className="edit-field">
              <label htmlFor="edit-name">Name</label>
              <input
                id="edit-name"
                className="edit-input"
                type="text"
                value={editData.name}
                onChange={e => handleEditChange('name', e.target.value)}
              />
            </div>
          ) : (
            <h1 className="product-name">{name}</h1>
          )}

          <div className="product-meta">
            <span className={`stock-badge ${stockKey}`}>
              {STOCK_LABELS[stockKey] || stockKey}
            </span>
            {product.isOrderable && (
              <span className="orderable-badge">Orderable</span>
            )}
          </div>

          {editing ? (
            <>
              <div className="edit-field">
                <label htmlFor="edit-short-desc">Short Description</label>
                <textarea
                  id="edit-short-desc"
                  className="edit-textarea"
                  value={editData.shortDescription}
                  onChange={e => handleEditChange('shortDescription', e.target.value)}
                />
              </div>
              <div className="edit-field">
                <label htmlFor="edit-long-desc">Long Description</label>
                <textarea
                  id="edit-long-desc"
                  className="edit-textarea"
                  style={{ minHeight: 90 }}
                  value={editData.longDescription}
                  onChange={e => handleEditChange('longDescription', e.target.value)}
                />
              </div>
              <div className="edit-field">
                <label htmlFor="edit-status">Status</label>
                <select
                  id="edit-status"
                  className="edit-select"
                  value={editData.status}
                  onChange={e => handleEditChange('status', e.target.value)}
                >
                  <option value="active">active</option>
                  <option value="draft">draft</option>
                  <option value="discontinued">discontinued</option>
                </select>
              </div>
              <div className="edit-field" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <input
                  id="edit-orderable"
                  type="checkbox"
                  checked={editData.isOrderable}
                  onChange={e => handleEditChange('isOrderable', e.target.checked)}
                />
                <label htmlFor="edit-orderable" style={{ marginBottom: 0, cursor: 'pointer' }}>
                  Orderable
                </label>
              </div>
            </>
          ) : (
            description && <p className="product-description">{description}</p>
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
        {editing ? (
          <EditableSpecsTable
            product={product}
            editData={editData}
            onChange={handleEditChange}
          />
        ) : (
          <SpecsTable product={product} />
        )}
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
