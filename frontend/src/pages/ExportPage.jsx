import { useState, useEffect, useCallback } from 'react';
import { exportProducts, getExportPreview, getOperations, getCategories, getFilterOptions } from '../api/api';
import { useToast } from '../components/ToastContext';

const FORMATS = [
  { value: 'csv', label: 'CSV', icon: '📄', desc: 'Comma-separated values' },
  { value: 'xlsx', label: 'Excel', icon: '📊', desc: 'Microsoft Excel' },
  { value: 'xml', label: 'XML', icon: '📋', desc: 'Structured XML data' },
];

const LOCALES_OPT = [
  { value: 'en', label: 'English' },
  { value: 'he', label: 'Hebrew' },
  { value: 'ru', label: 'Russian' },
];

const EMPTY_FILTERS = {
  toolMaterial: '',
  workpieceMaterial: '',
  machineType: '',
  machineBrand: '',
  cuttingType: '',
  productType: '',
  operation: '',
  inStock: '',
  dMmMin: '',
  dMmMax: '',
  shankMm: '',
  hasBallBearing: '',
  sectionId: '',
  categoryId: '',
  groupId: '',
};

export default function ExportPage({ locale: appLocale }) {
  const toast = useToast();

  const [format, setFormat] = useState('xlsx');
  const [locale, setLocale] = useState(appLocale || 'en');
  const [showFilters, setShowFilters] = useState(false);
  const [loading, setLoading] = useState(false);
  const [filters, setFilters] = useState(EMPTY_FILTERS);

  // Preview state
  const [preview, setPreview] = useState(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [previewPage, setPreviewPage] = useState(1);
  const [showPreview, setShowPreview] = useState(false);

  // Options for dropdowns
  const [operations, setOperations] = useState([]);
  const [categories, setCategories] = useState([]);
  const [filterOptions, setFilterOptions] = useState({});

  useEffect(() => {
    getOperations()
      .then(data => setOperations(Array.isArray(data) ? data : data.items || data.operations || []))
      .catch(() => {});
    getCategories(appLocale || 'en')
      .then(data => {
        const raw = Array.isArray(data) ? data : data.categories || [];
        setCategories(raw);
      })
      .catch(() => {});
    getFilterOptions()
      .then(setFilterOptions)
      .catch(() => {});
  }, [appLocale]);

  function handleFilterChange(key, value) {
    setFilters(prev => ({ ...prev, [key]: value }));
    setShowPreview(false);
    setPreview(null);
  }

  function clearFilters() {
    setFilters(EMPTY_FILTERS);
    setShowPreview(false);
    setPreview(null);
  }

  function getActiveFilters() {
    return Object.fromEntries(
      Object.entries(filters).filter(([, v]) => v !== '')
    );
  }

  const fetchPreview = useCallback(async (pg = 1) => {
    setPreviewLoading(true);
    try {
      const data = await getExportPreview(locale, getActiveFilters(), pg, 20);
      setPreview(data);
      setPreviewPage(pg);
      setShowPreview(true);
    } catch (err) {
      toast(`Preview failed: ${err.message}`, 'error');
    } finally {
      setPreviewLoading(false);
    }
  }, [locale, filters]);

  async function handleExport(e) {
    e.preventDefault();
    setLoading(true);
    try {
      await exportProducts(format, locale, getActiveFilters());
      toast(`Export started — your ${format.toUpperCase()} file is downloading.`, 'success');
    } catch (err) {
      toast(`Export failed: ${err.message}`, 'error');
    } finally {
      setLoading(false);
    }
  }

  const hasActiveFilters = Object.values(filters).some(v => v !== '');
  const totalCount = preview ? (preview.total || preview.totalElements || 0) : null;

  // Flatten sections → categories → groups for select
  const groupOptions = [];
  for (const section of categories) {
    for (const cat of section.categories || []) {
      for (const grp of cat.groups || []) {
        const name = grp.translations?.[locale] || grp.translations?.en || grp.slug || grp.groupCode || 'Unnamed';
        groupOptions.push({ id: grp.id, name });
      }
    }
  }

  const previewItems = preview ? (preview.items || preview.content || preview.products || []) : [];
  const totalPages = totalCount ? Math.ceil(totalCount / 20) : 0;

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Export Catalog</h1>
        <p className="page-subtitle">Download the product catalog in your preferred format</p>
      </div>

      <div className="export-layout">
        {/* Left column: settings + about */}
        <div className="export-left">
          <form className="card" onSubmit={handleExport}>
            <div className="card-title">Export Settings</div>

            <div className="export-form">
              {/* Format selection */}
              <div className="form-group">
                <label className="form-label">Format</label>
                <div className="export-format-group">
                  {FORMATS.map(f => (
                    <div key={f.value}>
                      <input
                        id={`fmt-${f.value}`}
                        type="radio"
                        name="format"
                        value={f.value}
                        className="format-radio"
                        checked={format === f.value}
                        onChange={() => setFormat(f.value)}
                      />
                      <label htmlFor={`fmt-${f.value}`} className="format-label" title={f.desc}>
                        <span className="format-icon">{f.icon}</span>
                        {f.label}
                      </label>
                    </div>
                  ))}
                </div>
              </div>

              {/* Locale */}
              <div className="form-group">
                <label className="form-label" htmlFor="export-locale">Language / Locale</label>
                <select
                  id="export-locale"
                  className="form-control"
                  value={locale}
                  onChange={e => setLocale(e.target.value)}
                >
                  {LOCALES_OPT.map(l => (
                    <option key={l.value} value={l.value}>{l.label}</option>
                  ))}
                </select>
              </div>

              {/* Filters toggle */}
              <button
                type="button"
                className="export-filters-toggle"
                onClick={() => setShowFilters(prev => !prev)}
              >
                {showFilters ? '▾' : '▸'} {showFilters ? 'Hide' : 'Show'} filters
                {hasActiveFilters && <span style={{ marginLeft: 6, color: 'var(--wpw-primary)', fontWeight: 600 }}>(active)</span>}
              </button>

              {showFilters && (
                <div className="export-optional-filters">
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>

                    <div className="form-group">
                      <label className="form-label">Application Tag</label>
                      <select
                        className="form-control"
                        value={filters.operation}
                        onChange={e => handleFilterChange('operation', e.target.value)}
                      >
                        <option value="">All</option>
                        {operations.map(op => (
                          <option key={op.code || op.id} value={op.code || op.id}>
                            {op.name || op.label || op.code}
                          </option>
                        ))}
                      </select>
                    </div>

                    <div className="form-group">
                      <label className="form-label">Product Type</label>
                      <select
                        className="form-control"
                        value={filters.productType}
                        onChange={e => handleFilterChange('productType', e.target.value)}
                      >
                        <option value="">All</option>
                        <option value="main">Main</option>
                        <option value="spare_part">Spare Part</option>
                        <option value="accessory">Accessory</option>
                      </select>
                    </div>

                    <div className="form-group">
                      <label className="form-label">Tool Material</label>
                      <select
                        className="form-control"
                        value={filters.toolMaterial}
                        onChange={e => handleFilterChange('toolMaterial', e.target.value)}
                      >
                        <option value="">All</option>
                        {(filterOptions.toolMaterial || []).map(v => (
                          <option key={v} value={v}>{v}</option>
                        ))}
                      </select>
                    </div>

                    <div className="form-group">
                      <label className="form-label">Workpiece Material</label>
                      <select
                        className="form-control"
                        value={filters.workpieceMaterial}
                        onChange={e => handleFilterChange('workpieceMaterial', e.target.value)}
                      >
                        <option value="">All</option>
                        {(filterOptions.workpieceMaterial || []).map(v => (
                          <option key={v} value={v}>{v}</option>
                        ))}
                      </select>
                    </div>

                    <div className="form-group">
                      <label className="form-label">Machine Type</label>
                      <select
                        className="form-control"
                        value={filters.machineType}
                        onChange={e => handleFilterChange('machineType', e.target.value)}
                      >
                        <option value="">All</option>
                        {(filterOptions.machineType || []).map(v => (
                          <option key={v} value={v}>{v}</option>
                        ))}
                      </select>
                    </div>

                    <div className="form-group">
                      <label className="form-label">Machine Brand</label>
                      <select
                        className="form-control"
                        value={filters.machineBrand}
                        onChange={e => handleFilterChange('machineBrand', e.target.value)}
                      >
                        <option value="">All</option>
                        {(filterOptions.machineBrand || []).map(v => (
                          <option key={v} value={v}>{v}</option>
                        ))}
                      </select>
                    </div>

                    <div className="form-group">
                      <label className="form-label">Cutting Type</label>
                      <select
                        className="form-control"
                        value={filters.cuttingType}
                        onChange={e => handleFilterChange('cuttingType', e.target.value)}
                      >
                        <option value="">All</option>
                        {(filterOptions.cuttingType || []).map(v => (
                          <option key={v} value={v}>{v}</option>
                        ))}
                      </select>
                    </div>

                    <div className="form-group">
                      <label className="form-label">Shank (mm)</label>
                      <select
                        className="form-control"
                        value={filters.shankMm}
                        onChange={e => handleFilterChange('shankMm', e.target.value)}
                      >
                        <option value="">All</option>
                        {(filterOptions.shankMm || []).map(v => (
                          <option key={v} value={v}>{v}</option>
                        ))}
                      </select>
                    </div>

                    <div className="form-group">
                      <label className="form-label">Diameter min (mm)</label>
                      <input
                        className="form-control"
                        type="number"
                        placeholder="Min"
                        value={filters.dMmMin}
                        onChange={e => handleFilterChange('dMmMin', e.target.value)}
                      />
                    </div>

                    <div className="form-group">
                      <label className="form-label">Diameter max (mm)</label>
                      <input
                        className="form-control"
                        type="number"
                        placeholder="Max"
                        value={filters.dMmMax}
                        onChange={e => handleFilterChange('dMmMax', e.target.value)}
                      />
                    </div>

                    <div className="form-group">
                      <label className="form-label">Availability</label>
                      <select
                        className="form-control"
                        value={filters.inStock}
                        onChange={e => handleFilterChange('inStock', e.target.value)}
                      >
                        <option value="">All</option>
                        <option value="true">In Stock Only</option>
                        <option value="false">Out of Stock Only</option>
                      </select>
                    </div>

                    <div className="form-group">
                      <label className="form-label">Ball Bearing</label>
                      <select
                        className="form-control"
                        value={filters.hasBallBearing}
                        onChange={e => handleFilterChange('hasBallBearing', e.target.value)}
                      >
                        <option value="">Any</option>
                        <option value="true">Yes</option>
                        <option value="false">No</option>
                      </select>
                    </div>

                  </div>
                  {hasActiveFilters && (
                    <button
                      type="button"
                      className="btn"
                      style={{ marginTop: 8, fontSize: 12 }}
                      onClick={clearFilters}
                    >
                      Clear all filters
                    </button>
                  )}
                </div>
              )}

              <div style={{ display: 'flex', gap: 10, alignItems: 'center', marginTop: 12 }}>
                <button
                  type="button"
                  className="btn"
                  onClick={() => fetchPreview(1)}
                  disabled={previewLoading}
                  style={{ minWidth: 120 }}
                >
                  {previewLoading ? 'Loading...' : 'Preview'}
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={loading}
                  style={{ minWidth: 140 }}
                >
                  {loading ? (
                    <>
                      <div className="spinner" style={{ width: 14, height: 14, borderWidth: 2 }} />
                      Preparing...
                    </>
                  ) : (
                    `Download ${format.toUpperCase()}`
                  )}
                </button>
                {totalCount !== null && (
                  <span style={{ fontSize: 13, color: 'var(--wpw-mid-gray)' }}>
                    {totalCount} product{totalCount !== 1 ? 's' : ''} will be exported
                  </span>
                )}
              </div>
            </div>
          </form>

          <div className="card" style={{ marginTop: 16 }}>
            <div className="card-title">About Export Formats</div>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
              <thead>
                <tr style={{ background: 'var(--wpw-light-gray)' }}>
                  <th style={{ padding: '8px 12px', textAlign: 'left', borderBottom: '1px solid var(--wpw-border)', fontWeight: 600 }}>Format</th>
                  <th style={{ padding: '8px 12px', textAlign: 'left', borderBottom: '1px solid var(--wpw-border)', fontWeight: 600 }}>Description</th>
                  <th style={{ padding: '8px 12px', textAlign: 'left', borderBottom: '1px solid var(--wpw-border)', fontWeight: 600 }}>Best for</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td style={{ padding: '8px 12px', borderBottom: '1px solid var(--wpw-border)' }}>CSV</td>
                  <td style={{ padding: '8px 12px', borderBottom: '1px solid var(--wpw-border)' }}>Comma-separated plain text</td>
                  <td style={{ padding: '8px 12px', borderBottom: '1px solid var(--wpw-border)' }}>Data import, scripts</td>
                </tr>
                <tr>
                  <td style={{ padding: '8px 12px', borderBottom: '1px solid var(--wpw-border)' }}>Excel</td>
                  <td style={{ padding: '8px 12px', borderBottom: '1px solid var(--wpw-border)' }}>Formatted spreadsheet</td>
                  <td style={{ padding: '8px 12px', borderBottom: '1px solid var(--wpw-border)' }}>Business reports</td>
                </tr>
                <tr>
                  <td style={{ padding: '8px 12px' }}>XML</td>
                  <td style={{ padding: '8px 12px' }}>Structured markup data</td>
                  <td style={{ padding: '8px 12px' }}>System integration</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        {/* Right column: preview */}
        {showPreview && (
          <div className="export-right">
            <div className="card export-preview-card">
              <div className="card-title">
                Export Preview
                {totalCount !== null && (
                  <span style={{ fontWeight: 400, fontSize: 13, marginLeft: 8, color: 'var(--wpw-mid-gray)' }}>
                    ({totalCount} total)
                  </span>
                )}
              </div>

              {previewLoading ? (
                <div style={{ padding: 24, textAlign: 'center', color: 'var(--wpw-mid-gray)' }}>Loading…</div>
              ) : previewItems.length === 0 ? (
                <p style={{ padding: 16, color: 'var(--wpw-mid-gray)' }}>No products match the selected filters.</p>
              ) : (
                <>
                  <div className="export-preview-list">
                    {previewItems.map((p, i) => {
                      const thumbUrl = p.thumbnailUrl || p.imageUrl || p.image_url || p.mainImageUrl;
                      return (
                        <div key={p.id || p.toolNo || p.tool_no || i} className="export-preview-row">
                          <div className="export-preview-thumb">
                            {thumbUrl ? (
                              <img src={thumbUrl} alt={p.toolNo || ''} loading="lazy"
                                onError={e => { e.currentTarget.style.visibility = 'hidden'; }}
                              />
                            ) : (
                              <div className="export-preview-thumb-placeholder" />
                            )}
                          </div>
                          <div className="export-preview-info">
                            <div className="export-preview-toolno">{p.toolNo || p.tool_no}</div>
                            <div className="export-preview-name">{p.name || p.title || '-'}</div>
                          </div>
                          <div className="export-preview-meta">
                            <span className="export-preview-type">{p.productType || p.product_type || ''}</span>
                            <span className="export-preview-status">{p.status || ''}</span>
                          </div>
                        </div>
                      );
                    })}
                  </div>

                  {totalPages > 1 && (
                    <div style={{ display: 'flex', justifyContent: 'center', gap: 8, padding: 12, borderTop: '1px solid var(--wpw-border)' }}>
                      <button
                        className="btn"
                        disabled={previewPage <= 1 || previewLoading}
                        onClick={() => fetchPreview(previewPage - 1)}
                        style={{ fontSize: 12, padding: '4px 12px' }}
                      >
                        Prev
                      </button>
                      <span style={{ fontSize: 13, lineHeight: '28px' }}>
                        Page {previewPage} of {totalPages}
                      </span>
                      <button
                        className="btn"
                        disabled={previewPage >= totalPages || previewLoading}
                        onClick={() => fetchPreview(previewPage + 1)}
                        style={{ fontSize: 12, padding: '4px 12px' }}
                      >
                        Next
                      </button>
                    </div>
                  )}
                </>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
