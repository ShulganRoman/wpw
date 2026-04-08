import { useState } from 'react';

const EMPTY_ATTRS = {
  dMm: '', d1Mm: '', d2Mm: '', bMm: '', b1Mm: '',
  lMm: '', l1Mm: '', rMm: '', aMm: '', angleDeg: '',
  shankMm: '', shankInch: '', flutes: '', bladeNo: '',
  cuttingType: '', rotationDirection: '', boreType: '',
  hasBallBearing: false, ballBearingCode: '',
  hasRetainer: false, retainerCode: '',
  canResharpen: false,
  ean13: '', upc12: '', hsCode: '', countryOfOrigin: '',
  weightG: '', pkgQty: '', cartonQty: '',
  stockStatus: '', stockQty: '',
};

const EMPTY_FORM = {
  toolNo: '',
  altToolNo: '',
  productType: 'main',
  status: 'active',
  isOrderable: true,
  catalogPage: '',
  name: '',
  shortDescription: '',
  longDescription: '',
  seoTitle: '',
  seoDescription: '',
  applications: '',
  toolMaterials: '',
  workpieceMaterials: '',
  machineTypes: '',
  machineBrands: '',
  operationCodes: '',
  attributes: { ...EMPTY_ATTRS },
};

function Section({ title, children, defaultOpen = false }) {
  const [open, setOpen] = useState(defaultOpen);
  return (
    <div style={{ borderBottom: '1px solid var(--wpw-border)', marginBottom: 0 }}>
      <button
        type="button"
        onClick={() => setOpen(v => !v)}
        style={{
          width: '100%', display: 'flex', alignItems: 'center', gap: 8,
          background: 'none', border: 'none', padding: '10px 0', cursor: 'pointer',
          fontSize: 12, fontWeight: 700, color: 'var(--wpw-navy)',
          textTransform: 'uppercase', letterSpacing: '0.05em',
        }}
      >
        <span style={{
          display: 'inline-block', fontSize: 10,
          transform: open ? 'rotate(90deg)' : 'rotate(0)',
          transition: 'transform 0.15s',
        }}>&#9654;</span>
        {title}
      </button>
      {open && <div style={{ paddingBottom: 14, display: 'flex', flexDirection: 'column', gap: 10 }}>{children}</div>}
    </div>
  );
}

function Row({ label, children }) {
  return (
    <div style={{ display: 'grid', gridTemplateColumns: '140px 1fr', gap: 8, alignItems: 'center' }}>
      <label style={{ fontSize: 12, color: 'var(--wpw-gray)', textAlign: 'right', paddingRight: 4 }}>{label}</label>
      {children}
    </div>
  );
}

const inputStyle = {
  padding: '5px 8px', border: '1px solid var(--wpw-border)', borderRadius: 'var(--wpw-radius-sm)',
  fontSize: 13, color: 'var(--wpw-navy)', background: '#fff', width: '100%', boxSizing: 'border-box',
};

const selectStyle = { ...inputStyle };

function FInput({ value, onChange, type = 'text', placeholder, step }) {
  return (
    <input
      style={inputStyle}
      type={type}
      step={step}
      value={value}
      onChange={e => onChange(e.target.value)}
      placeholder={placeholder}
    />
  );
}

function FSelect({ value, onChange, options }) {
  return (
    <select style={selectStyle} value={value} onChange={e => onChange(e.target.value)}>
      {options.map(([v, label]) => <option key={v} value={v}>{label}</option>)}
    </select>
  );
}

function FTextarea({ value, onChange, rows = 3, placeholder }) {
  return (
    <textarea
      style={{ ...inputStyle, resize: 'vertical' }}
      rows={rows}
      value={value}
      onChange={e => onChange(e.target.value)}
      placeholder={placeholder}
    />
  );
}

function FCheck({ checked, onChange, label }) {
  return (
    <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 13, color: 'var(--wpw-gray)', cursor: 'pointer' }}>
      <input type="checkbox" checked={checked} onChange={e => onChange(e.target.checked)} />
      {label}
    </label>
  );
}

export default function ProductCreateModal({ groupId, groupName, onSave, onCancel }) {
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  function set(key, value) {
    setForm(prev => ({ ...prev, [key]: value }));
  }

  function setAttr(key, value) {
    setForm(prev => ({ ...prev, attributes: { ...prev.attributes, [key]: value } }));
  }

  function parseCollection(str) {
    return str.split(',').map(s => s.trim()).filter(Boolean);
  }

  function parseNum(val) {
    if (val === '' || val === null || val === undefined) return null;
    const n = Number(val);
    return isNaN(n) ? null : n;
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (!form.toolNo.trim()) { setError('SKU (toolNo) is required'); return; }
    setError('');
    setSaving(true);

    const a = form.attributes;
    const attrs = {
      dMm: parseNum(a.dMm), d1Mm: parseNum(a.d1Mm), d2Mm: parseNum(a.d2Mm),
      bMm: parseNum(a.bMm), b1Mm: parseNum(a.b1Mm),
      lMm: parseNum(a.lMm), l1Mm: parseNum(a.l1Mm),
      rMm: parseNum(a.rMm), aMm: parseNum(a.aMm), angleDeg: parseNum(a.angleDeg),
      shankMm: parseNum(a.shankMm), shankInch: a.shankInch || null,
      flutes: parseNum(a.flutes), bladeNo: parseNum(a.bladeNo),
      cuttingType: a.cuttingType || null,
      rotationDirection: a.rotationDirection || null,
      boreType: a.boreType || null,
      hasBallBearing: !!a.hasBallBearing, ballBearingCode: a.ballBearingCode || null,
      hasRetainer: !!a.hasRetainer, retainerCode: a.retainerCode || null,
      canResharpen: !!a.canResharpen,
      ean13: a.ean13 || null, upc12: a.upc12 || null,
      hsCode: a.hsCode || null, countryOfOrigin: a.countryOfOrigin || null,
      weightG: parseNum(a.weightG), pkgQty: parseNum(a.pkgQty), cartonQty: parseNum(a.cartonQty),
      stockStatus: a.stockStatus || null, stockQty: parseNum(a.stockQty),
    };

    const hasAnyAttr = Object.values(attrs).some(v => v !== null && v !== false && v !== '');

    const payload = {
      toolNo: form.toolNo.trim(),
      groupId,
      altToolNo: form.altToolNo || null,
      productType: form.productType,
      status: form.status,
      isOrderable: form.isOrderable,
      catalogPage: parseNum(form.catalogPage),
      name: form.name || null,
      shortDescription: form.shortDescription || null,
      longDescription: form.longDescription || null,
      seoTitle: form.seoTitle || null,
      seoDescription: form.seoDescription || null,
      applications: form.applications || null,
      attributes: hasAnyAttr ? attrs : null,
      toolMaterials: parseCollection(form.toolMaterials),
      workpieceMaterials: parseCollection(form.workpieceMaterials),
      machineTypes: parseCollection(form.machineTypes),
      machineBrands: parseCollection(form.machineBrands),
      operationCodes: parseCollection(form.operationCodes),
    };

    try {
      await onSave(payload);
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  const a = form.attributes;

  return (
    <div
      style={{
        position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.45)', zIndex: 1000,
        display: 'flex', alignItems: 'flex-start', justifyContent: 'center',
        padding: '24px 16px', overflowY: 'auto',
      }}
      onClick={e => e.target === e.currentTarget && onCancel()}
    >
      <div style={{
        background: '#fff', borderRadius: 10, width: 640, maxWidth: '100%',
        boxShadow: '0 8px 32px rgba(0,0,0,0.18)', flexShrink: 0,
      }}>
        {/* Header */}
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          padding: '16px 20px', borderBottom: '1px solid var(--wpw-border)',
        }}>
          <div>
            <div style={{ fontSize: 15, fontWeight: 700, color: 'var(--wpw-navy)' }}>Create Product</div>
            {groupName && (
              <div style={{ fontSize: 12, color: 'var(--wpw-mid-gray)', marginTop: 2 }}>
                Group: <strong>{groupName}</strong>
              </div>
            )}
          </div>
          <button
            type="button"
            onClick={onCancel}
            style={{ background: 'none', border: 'none', fontSize: 20, cursor: 'pointer', color: 'var(--wpw-mid-gray)', lineHeight: 1 }}
          >
            ×
          </button>
        </div>

        {/* Body */}
        <form onSubmit={handleSubmit}>
          <div style={{ padding: '4px 20px 0', maxHeight: '70vh', overflowY: 'auto' }}>

            {/* Basic */}
            <Section title="Basic Info" defaultOpen>
              <Row label="SKU *">
                <FInput value={form.toolNo} onChange={v => set('toolNo', v)} placeholder="e.g. WP-1234" />
              </Row>
              <Row label="Alt SKU">
                <FInput value={form.altToolNo} onChange={v => set('altToolNo', v)} placeholder="optional" />
              </Row>
              <Row label="Product Type">
                <FSelect value={form.productType} onChange={v => set('productType', v)} options={[
                  ['main', 'main'], ['spare_part', 'spare_part'], ['accessory', 'accessory'],
                ]} />
              </Row>
              <Row label="Status">
                <FSelect value={form.status} onChange={v => set('status', v)} options={[
                  ['active', 'active'], ['discontinued', 'discontinued'], ['coming_soon', 'coming_soon'],
                ]} />
              </Row>
              <Row label="">
                <FCheck checked={form.isOrderable} onChange={v => set('isOrderable', v)} label="Orderable" />
              </Row>
              <Row label="Catalog Page">
                <FInput value={form.catalogPage} onChange={v => set('catalogPage', v)} type="number" step="1" />
              </Row>
            </Section>

            {/* Translation EN */}
            <Section title="Content (EN)">
              <Row label="Name">
                <FInput value={form.name} onChange={v => set('name', v)} placeholder="Product name" />
              </Row>
              <Row label="Short Description">
                <FTextarea value={form.shortDescription} onChange={v => set('shortDescription', v)} rows={2} />
              </Row>
              <Row label="Long Description">
                <FTextarea value={form.longDescription} onChange={v => set('longDescription', v)} rows={4} />
              </Row>
              <Row label="Applications">
                <FTextarea value={form.applications} onChange={v => set('applications', v)} rows={2} />
              </Row>
              <Row label="SEO Title">
                <FInput value={form.seoTitle} onChange={v => set('seoTitle', v)} />
              </Row>
              <Row label="SEO Description">
                <FTextarea value={form.seoDescription} onChange={v => set('seoDescription', v)} rows={2} />
              </Row>
            </Section>

            {/* Dimensions */}
            <Section title="Dimensions">
              {[
                ['D (mm)', 'dMm'], ['D1 (mm)', 'd1Mm'], ['D2 (mm)', 'd2Mm'],
                ['B / Cut Length (mm)', 'bMm'], ['B1 (mm)', 'b1Mm'],
                ['L / Total (mm)', 'lMm'], ['L1 (mm)', 'l1Mm'],
                ['R (mm)', 'rMm'], ['A (mm)', 'aMm'], ['Angle (°)', 'angleDeg'],
                ['Shank (mm)', 'shankMm'],
              ].map(([label, key]) => (
                <Row key={key} label={label}>
                  <FInput value={a[key]} onChange={v => setAttr(key, v)} type="number" step="any" />
                </Row>
              ))}
              <Row label="Shank (inch)">
                <FInput value={a.shankInch} onChange={v => setAttr('shankInch', v)} />
              </Row>
              <Row label="Flutes">
                <FInput value={a.flutes} onChange={v => setAttr('flutes', v)} type="number" step="1" />
              </Row>
              <Row label="Blade No">
                <FInput value={a.bladeNo} onChange={v => setAttr('bladeNo', v)} type="number" step="1" />
              </Row>
            </Section>

            {/* Technical */}
            <Section title="Technical">
              <Row label="Cutting Type">
                <FInput value={a.cuttingType} onChange={v => setAttr('cuttingType', v)} />
              </Row>
              <Row label="Rotation Direction">
                <FSelect value={a.rotationDirection || ''} onChange={v => setAttr('rotationDirection', v || null)} options={[
                  ['', '—'], ['right', 'right'], ['left', 'left'], ['both', 'both'],
                ]} />
              </Row>
              <Row label="Bore Type">
                <FSelect value={a.boreType || ''} onChange={v => setAttr('boreType', v || null)} options={[
                  ['', '—'], ['shank', 'shank'], ['bore', 'bore'],
                ]} />
              </Row>
              <Row label="">
                <FCheck checked={a.hasBallBearing} onChange={v => setAttr('hasBallBearing', v)} label="Has Ball Bearing" />
              </Row>
              {a.hasBallBearing && (
                <Row label="Ball Bearing Code">
                  <FInput value={a.ballBearingCode} onChange={v => setAttr('ballBearingCode', v)} />
                </Row>
              )}
              <Row label="">
                <FCheck checked={a.hasRetainer} onChange={v => setAttr('hasRetainer', v)} label="Has Retainer" />
              </Row>
              {a.hasRetainer && (
                <Row label="Retainer Code">
                  <FInput value={a.retainerCode} onChange={v => setAttr('retainerCode', v)} />
                </Row>
              )}
              <Row label="">
                <FCheck checked={a.canResharpen} onChange={v => setAttr('canResharpen', v)} label="Can Resharpen" />
              </Row>
            </Section>

            {/* Collections */}
            <Section title="Materials & Operations">
              {[
                ['Tool Materials', 'toolMaterials'],
                ['Workpiece Materials', 'workpieceMaterials'],
                ['Machine Types', 'machineTypes'],
                ['Machine Brands', 'machineBrands'],
                ['Application Tags', 'operationCodes'],
              ].map(([label, key]) => (
                <Row key={key} label={label}>
                  <FInput
                    value={form[key]}
                    onChange={v => set(key, v)}
                    placeholder="comma-separated codes"
                  />
                </Row>
              ))}
            </Section>

            {/* Logistics */}
            <Section title="Logistics & Stock">
              <Row label="EAN-13">
                <FInput value={a.ean13} onChange={v => setAttr('ean13', v)} />
              </Row>
              <Row label="UPC-12">
                <FInput value={a.upc12} onChange={v => setAttr('upc12', v)} />
              </Row>
              <Row label="HS Code">
                <FInput value={a.hsCode} onChange={v => setAttr('hsCode', v)} />
              </Row>
              <Row label="Country of Origin">
                <FInput value={a.countryOfOrigin} onChange={v => setAttr('countryOfOrigin', v)} />
              </Row>
              <Row label="Weight (g)">
                <FInput value={a.weightG} onChange={v => setAttr('weightG', v)} type="number" step="1" />
              </Row>
              <Row label="Package Qty">
                <FInput value={a.pkgQty} onChange={v => setAttr('pkgQty', v)} type="number" step="1" />
              </Row>
              <Row label="Carton Qty">
                <FInput value={a.cartonQty} onChange={v => setAttr('cartonQty', v)} type="number" step="1" />
              </Row>
              <Row label="Stock Status">
                <FSelect value={a.stockStatus || ''} onChange={v => setAttr('stockStatus', v || null)} options={[
                  ['', '—'], ['in_stock', 'In Stock'], ['low_stock', 'Low Stock'],
                  ['out_of_stock', 'Out of Stock'], ['on_order', 'On Order'],
                ]} />
              </Row>
              <Row label="Stock Qty">
                <FInput value={a.stockQty} onChange={v => setAttr('stockQty', v)} type="number" step="1" />
              </Row>
            </Section>

          </div>

          {/* Footer */}
          {error && (
            <div style={{ margin: '12px 20px 0', fontSize: 12, color: '#c62828', background: '#fdecea', borderRadius: 6, padding: '8px 12px' }}>
              {error}
            </div>
          )}
          <div style={{
            display: 'flex', gap: 8, justifyContent: 'flex-end',
            padding: '14px 20px', borderTop: '1px solid var(--wpw-border)', marginTop: 12,
          }}>
            <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={saving}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? (
                <><div className="spinner" style={{ width: 14, height: 14, borderWidth: 2 }} /> Creating…</>
              ) : 'Create Product'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
