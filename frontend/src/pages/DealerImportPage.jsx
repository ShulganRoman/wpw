import { useState, useEffect, useRef } from 'react';
import { useToast } from '../components/ToastContext';
import { useImportSession } from '../contexts/SessionContext';
import { useLocale } from '../contexts/LocaleContext';

const BASE = '/api/v1';

function authHeaders() {
  const token = localStorage.getItem('authToken');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function apiGet(path) {
  const res = await fetch(`${BASE}${path}`, { headers: { ...authHeaders() } });
  if (!res.ok) throw new Error(await res.text() || `HTTP ${res.status}`);
  return res.json();
}

async function apiPut(path, body) {
  const res = await fetch(`${BASE}${path}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(await res.text() || `HTTP ${res.status}`);
  return res.json();
}

async function apiDelete(path) {
  const res = await fetch(`${BASE}${path}`, { method: 'DELETE', headers: { ...authHeaders() } });
  if (!res.ok) throw new Error(await res.text() || `HTTP ${res.status}`);
}

async function apiUpload(path, file, params = {}) {
  const form = new FormData();
  form.append('file', file);
  const query = new URLSearchParams(params).toString();
  const res = await fetch(`${BASE}${path}${query ? '?' + query : ''}`, {
    method: 'POST',
    headers: { ...authHeaders() },
    body: form,
  });
  if (!res.ok) throw new Error(await res.text() || `HTTP ${res.status}`);
  return res.json();
}

async function downloadFile(path, filename) {
  const res = await fetch(`${BASE}${path}`, { headers: { ...authHeaders() } });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url; a.download = filename; a.click();
  URL.revokeObjectURL(url);
}

// ── Shared styles ─────────────────────────────────────────────────────────────
const thS = { padding: '8px 12px', textAlign: 'left', fontSize: 11, fontWeight: 700, color: 'var(--wpw-gray)', textTransform: 'uppercase', letterSpacing: '0.04em' };
const tdS = { padding: '9px 12px', fontSize: 13 };

// ── Import panel (validate → report → execute) ────────────────────────────────
function ImportPanel({ file, setFile, report, setReport, onImported }) {
  const toast = useToast();
  const { t } = useLocale();
  const fileRef = useRef();
  const [loading, setLoading] = useState(false);

  async function handleValidate() {
    if (!file) { toast(t('select_file_warning'), 'warning'); return; }
    setLoading(true);
    try {
      const r = await apiUpload('/dealer/sku-mapping/validate', file);
      setReport(r);
    } catch (e) { toast(e.message, 'error'); }
    finally { setLoading(false); }
  }

  async function handleExecute(skipGhosts) {
    setLoading(true);
    try {
      const result = await apiUpload('/dealer/sku-mapping/execute', file, { skipGhosts });
      toast(`Imported: ${result.imported} (created ${result.created}, updated ${result.updated})`, 'success');
      setReport(null); setFile(null);
      if (fileRef.current) fileRef.current.value = '';
      onImported?.();
    } catch (e) { toast(e.message, 'error'); }
    finally { setLoading(false); }
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div style={{ display: 'flex', gap: 10, alignItems: 'center', flexWrap: 'wrap' }}>
        <input ref={fileRef} type="file" accept=".xlsx,.xls"
          onChange={e => { setFile(e.target.files[0] || null); }}
          style={{ fontSize: 13 }} />
        {file && <span style={{ fontSize: 12, color: 'var(--wpw-text-secondary)' }}>{file.name}</span>}
        <button className="btn btn-primary" onClick={handleValidate} disabled={loading || !file}>
          {loading ? t('processing') : t('validate_file')}
        </button>
        <button className="btn btn-secondary" onClick={() => downloadFile('/dealer/sku-mapping/template', 'sku-mapping-template.xlsx')}
          style={{ fontSize: 12 }}>
          {t('download_template_dealer')}
        </button>
      </div>

      {report && (
        <div style={{ border: '1px solid var(--wpw-border)', borderRadius: 8, overflow: 'hidden' }}>
          {/* summary */}
          <div style={{ padding: '12px 16px', background: '#f5f7fa', borderBottom: '1px solid var(--wpw-border)', display: 'flex', gap: 24, flexWrap: 'wrap' }}>
            <Stat label={t('stat_total_rows')} value={report.total} />
            <Stat label={t('stat_found_in_catalog')} value={report.valid.length} color="#2e7d32" />
            <Stat label={t('stat_ghost_skus')} value={report.ghosts.length} color={report.ghosts.length > 0 ? '#e65100' : '#2e7d32'} />
            {report.errors.length > 0 && <Stat label={t('stat_format_errors')} value={report.errors.length} color="#c62828" />}
          </div>

          {/* ghost list */}
          {report.ghosts.length > 0 && (
            <div style={{ padding: '10px 16px', background: '#fff8e1', borderBottom: '1px solid var(--wpw-border)' }}>
              <div style={{ fontSize: 12, fontWeight: 600, color: '#e65100', marginBottom: 6 }}>
                {t('skus_not_found')}
              </div>
              <div style={{ fontSize: 12, fontFamily: 'monospace', color: '#6d4c00', display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                {report.ghosts.map(g => (
                  <span key={g.wpwSku} style={{ background: '#ffe082', padding: '2px 6px', borderRadius: 4 }}>{g.wpwSku}</span>
                ))}
              </div>
            </div>
          )}

          {/* format errors */}
          {report.errors.length > 0 && (
            <div style={{ padding: '10px 16px', background: '#ffebee', borderBottom: '1px solid var(--wpw-border)' }}>
              {report.errors.map((e, i) => <div key={i} style={{ fontSize: 12, color: '#c62828' }}>• {e}</div>)}
            </div>
          )}

          {/* action buttons */}
          <div style={{ padding: '12px 16px', display: 'flex', gap: 10, flexWrap: 'wrap' }}>
            <button className="btn btn-primary" onClick={() => handleExecute(true)} disabled={loading || report.valid.length === 0}>
              {t('import_without_ghosts', { count: report.valid.length })}
            </button>
            <button className="btn btn-secondary" onClick={() => handleExecute(false)} disabled={loading}>
              {t('import_all', { count: report.total - report.errors.length })}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

function Stat({ label, value, color }) {
  return (
    <div>
      <div style={{ fontSize: 11, color: 'var(--wpw-mid-gray)' }}>{label}</div>
      <div style={{ fontSize: 20, fontWeight: 700, color: color || 'var(--wpw-navy)' }}>{value}</div>
    </div>
  );
}

// ── SKU Mapping table with inline editing ─────────────────────────────────────
function SkuMappingTable({ rows, onUpsert, onDelete }) {
  const toast = useToast();
  const { t } = useLocale();
  const [editKey, setEditKey] = useState(null); // wpwSku being edited
  const [editForm, setEditForm] = useState({});
  const [newRow, setNewRow] = useState({ wpwSku: '', dealerSku: '', dealerBrand: '' });
  const [adding, setAdding] = useState(false);
  const [saving, setSaving] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(null);

  function startEdit(row) {
    setEditKey(row.wpwSku);
    setEditForm({ dealerSku: row.dealerSku || '', dealerBrand: row.dealerBrand || '' });
  }

  async function saveEdit(wpwSku) {
    setSaving(true);
    try {
      await onUpsert({ wpwSku, ...editForm });
      setEditKey(null);
    } catch (e) { toast(e.message, 'error'); }
    finally { setSaving(false); }
  }

  async function saveNew() {
    if (!newRow.wpwSku.trim() || !newRow.dealerSku.trim()) {
      toast(t('sku_required'), 'warning'); return;
    }
    setSaving(true);
    try {
      await onUpsert(newRow);
      setNewRow({ wpwSku: '', dealerSku: '', dealerBrand: '' });
      setAdding(false);
    } catch (e) { toast(e.message, 'error'); }
    finally { setSaving(false); }
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 10 }}>
        {!adding && (
          <button className="btn btn-secondary" style={{ fontSize: 12 }} onClick={() => setAdding(true)}>
            {t('add_row')}
          </button>
        )}
      </div>
      <div style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
          <thead>
            <tr style={{ borderBottom: '2px solid #e8edf5' }}>
              {[t('col_wpw_sku'), t('col_dealer_sku'), t('col_brand'), ''].map(h => <th key={h} style={thS}>{h}</th>)}
            </tr>
          </thead>
          <tbody>
            {adding && (
              <tr style={{ background: '#f0f7ff' }}>
                <td style={tdS}><input className="input" style={{ width: '100%' }} placeholder="WPW-001" value={newRow.wpwSku} onChange={e => setNewRow(f => ({ ...f, wpwSku: e.target.value }))} autoFocus /></td>
                <td style={tdS}><input className="input" style={{ width: '100%' }} placeholder="MY-SKU" value={newRow.dealerSku} onChange={e => setNewRow(f => ({ ...f, dealerSku: e.target.value }))} /></td>
                <td style={tdS}><input className="input" style={{ width: '100%' }} placeholder="Brand (optional)" value={newRow.dealerBrand} onChange={e => setNewRow(f => ({ ...f, dealerBrand: e.target.value }))} /></td>
                <td style={{ ...tdS, whiteSpace: 'nowrap' }}>
                  <button className="btn btn-primary" style={{ fontSize: 12, padding: '4px 10px' }} onClick={saveNew} disabled={saving}>{t('save')}</button>
                  <button className="btn btn-secondary" style={{ fontSize: 12, padding: '4px 10px', marginLeft: 6 }} onClick={() => setAdding(false)}>{t('cancel')}</button>
                </td>
              </tr>
            )}
            {rows.map(row => (
              <tr key={row.wpwSku} style={{ borderBottom: '1px solid #f0f2f5' }}>
                <td style={{ ...tdS, fontFamily: 'monospace', color: 'var(--wpw-mid-gray)' }}>{row.wpwSku}</td>
                {editKey === row.wpwSku ? (
                  <>
                    <td style={tdS}><input className="input" style={{ width: '100%' }} value={editForm.dealerSku} onChange={e => setEditForm(f => ({ ...f, dealerSku: e.target.value }))} autoFocus /></td>
                    <td style={tdS}><input className="input" style={{ width: '100%' }} value={editForm.dealerBrand} onChange={e => setEditForm(f => ({ ...f, dealerBrand: e.target.value }))} /></td>
                    <td style={{ ...tdS, whiteSpace: 'nowrap' }}>
                      <button className="btn btn-primary" style={{ fontSize: 12, padding: '4px 10px' }} onClick={() => saveEdit(row.wpwSku)} disabled={saving}>{t('save')}</button>
                      <button className="btn btn-secondary" style={{ fontSize: 12, padding: '4px 10px', marginLeft: 6 }} onClick={() => setEditKey(null)}>{t('cancel')}</button>
                    </td>
                  </>
                ) : (
                  <>
                    <td style={{ ...tdS, fontFamily: 'monospace' }}>{row.dealerSku}</td>
                    <td style={tdS}>{row.dealerBrand || '—'}</td>
                    <td style={{ ...tdS, whiteSpace: 'nowrap' }}>
                      {confirmDelete === row.wpwSku ? (
                        <span style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                          <span style={{ fontSize: 12, color: '#c62828' }}>{t('delete_confirm')}</span>
                          <button className="btn btn-primary" style={{ fontSize: 12, padding: '3px 8px', background: '#c62828', borderColor: '#c62828' }} onClick={() => { onDelete(row.wpwSku); setConfirmDelete(null); }}>{t('yes')}</button>
                          <button className="btn btn-secondary" style={{ fontSize: 12, padding: '3px 8px' }} onClick={() => setConfirmDelete(null)}>{t('no')}</button>
                        </span>
                      ) : (
                        <span style={{ display: 'flex', gap: 6 }}>
                          <button className="btn btn-secondary" style={{ fontSize: 12, padding: '4px 10px' }} onClick={() => startEdit(row)} disabled={editKey !== null}>{t('edit')}</button>
                          <button className="btn btn-secondary" style={{ fontSize: 12, padding: '4px 10px', color: '#c62828' }} onClick={() => setConfirmDelete(row.wpwSku)} disabled={editKey !== null}>{t('delete')}</button>
                        </span>
                      )}
                    </td>
                  </>
                )}
              </tr>
            ))}
            {rows.length === 0 && !adding && (
              <tr><td colSpan={4} style={{ padding: '32px 12px', textAlign: 'center', color: 'var(--wpw-mid-gray)', fontSize: 13 }}>{t('no_mappings')}</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ── Main page ─────────────────────────────────────────────────────────────────
export default function DealerImportPage() {
  const toast = useToast();
  const { t } = useLocale();
  const { tab, setTab, file, setFile, report, setReport } = useImportSession();
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => { loadMapping(); }, []);

  async function loadMapping() {
    setLoading(true);
    try { setRows(await apiGet('/dealer/sku-mapping')); }
    catch (e) { toast(e.message, 'error'); }
    finally { setLoading(false); }
  }

  async function handleUpsert(row) {
    const saved = await apiPut('/dealer/sku-mapping', row);
    setRows(prev => {
      const exists = prev.some(r => r.wpwSku === saved.wpwSku);
      return exists ? prev.map(r => r.wpwSku === saved.wpwSku ? saved : r) : [...prev, saved];
    });
  }

  async function handleDelete(wpwSku) {
    try {
      await apiDelete(`/dealer/sku-mapping/${encodeURIComponent(wpwSku)}`);
      setRows(prev => prev.filter(r => r.wpwSku !== wpwSku));
      toast(t('row_deleted'), 'success');
    } catch (e) { toast(e.message, 'error'); }
  }

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">{t('import_title')}</h1>
        <p className="page-subtitle">{t('import_subtitle')}</p>
      </div>

      <div style={{ display: 'flex', gap: 8, marginBottom: 24, flexWrap: 'wrap' }}>
        <button className={`btn ${tab === 'mapping' ? 'btn-primary' : ''}`} onClick={() => setTab('mapping')}>{t('tab_sku_mapping')}</button>
        <button className={`btn ${tab === 'import' ? 'btn-primary' : ''}`} onClick={() => setTab('import')}>{t('tab_excel_import')}</button>
      </div>

      {tab === 'mapping' && (
        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
            <div style={{ fontSize: 13, color: 'var(--wpw-mid-gray)' }}>
              {t('sku_records', { count: rows.length })}
            </div>
            <button className="btn btn-secondary" style={{ fontSize: 12 }}
              onClick={() => downloadFile('/dealer/sku-mapping/export', 'my-sku-mapping.xlsx')}>
              {t('export_excel')}
            </button>
          </div>
          {loading
            ? <div style={{ textAlign: 'center', padding: 40, color: 'var(--wpw-mid-gray)' }}><div className="spinner" style={{ margin: '0 auto 12px' }} />{t('loading')}</div>
            : <SkuMappingTable rows={rows} onUpsert={handleUpsert} onDelete={handleDelete} />
          }
        </div>
      )}

      {tab === 'import' && (
        <ImportPanel
          file={file} setFile={setFile}
          report={report} setReport={setReport}
          onImported={loadMapping}
        />
      )}
    </div>
  );
}
