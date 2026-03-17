import { useState } from 'react';
import { exportProducts } from '../api/api';
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

export default function ExportPage({ locale: appLocale }) {
  const toast = useToast();

  const [format, setFormat] = useState('xlsx');
  const [locale, setLocale] = useState(appLocale || 'en');
  const [showFilters, setShowFilters] = useState(false);
  const [loading, setLoading] = useState(false);
  const [extraFilters, setExtraFilters] = useState({
    toolMaterial: '',
    machineType: '',
    productType: '',
    inStock: '',
    operation: '',
  });

  function handleFilterChange(key, value) {
    setExtraFilters(prev => ({ ...prev, [key]: value }));
  }

  async function handleExport(e) {
    e.preventDefault();
    setLoading(true);
    try {
      const activeFilters = Object.fromEntries(
        Object.entries(extraFilters).filter(([, v]) => v !== '')
      );
      await exportProducts(format, locale, activeFilters);
      toast(`Export started — your ${format.toUpperCase()} file is downloading.`, 'success');
    } catch (err) {
      toast(`Export failed: ${err.message}`, 'error');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Export Catalog</h1>
        <p className="page-subtitle">Download the product catalog in your preferred format</p>
      </div>

      <form className="card" style={{ maxWidth: 560 }} onSubmit={handleExport}>
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

          {/* Optional filters toggle */}
          <button
            type="button"
            className="export-filters-toggle"
            onClick={() => setShowFilters(prev => !prev)}
          >
            {showFilters ? '▾' : '▸'} {showFilters ? 'Hide' : 'Show'} optional filters
          </button>

          {showFilters && (
            <div className="export-optional-filters">
              <div className="form-group">
                <label className="form-label">Tool Material</label>
                <input
                  className="form-control"
                  type="text"
                  placeholder="e.g. HSS, Carbide"
                  value={extraFilters.toolMaterial}
                  onChange={e => handleFilterChange('toolMaterial', e.target.value)}
                />
              </div>
              <div className="form-group">
                <label className="form-label">Machine Type</label>
                <input
                  className="form-control"
                  type="text"
                  placeholder="e.g. CNC, Manual"
                  value={extraFilters.machineType}
                  onChange={e => handleFilterChange('machineType', e.target.value)}
                />
              </div>
              <div className="form-group">
                <label className="form-label">Product Type</label>
                <input
                  className="form-control"
                  type="text"
                  placeholder="Product type"
                  value={extraFilters.productType}
                  onChange={e => handleFilterChange('productType', e.target.value)}
                />
              </div>
              <div className="form-group">
                <label className="form-label">Operation</label>
                <input
                  className="form-control"
                  type="text"
                  placeholder="Operation code"
                  value={extraFilters.operation}
                  onChange={e => handleFilterChange('operation', e.target.value)}
                />
              </div>
              <div className="form-group">
                <label className="form-label">Availability</label>
                <select
                  className="form-control"
                  value={extraFilters.inStock}
                  onChange={e => handleFilterChange('inStock', e.target.value)}
                >
                  <option value="">All</option>
                  <option value="true">In Stock Only</option>
                  <option value="false">Out of Stock Only</option>
                </select>
              </div>
            </div>
          )}

          <div style={{ display: 'flex', gap: 10, alignItems: 'center', marginTop: 8 }}>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={loading}
              style={{ minWidth: 140 }}
            >
              {loading ? (
                <>
                  <div className="spinner" style={{ width: 14, height: 14, borderWidth: 2 }} />
                  Preparing…
                </>
              ) : (
                `⬇ Download ${format.toUpperCase()}`
              )}
            </button>
            {loading && (
              <span style={{ fontSize: 12, color: 'var(--wpw-mid-gray)' }}>
                Generating file, please wait…
              </span>
            )}
          </div>
        </div>
      </form>

      <div className="card" style={{ maxWidth: 560, marginTop: 16 }}>
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
  );
}
