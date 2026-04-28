import { useState, useEffect, useRef } from 'react';
import { useToast } from './ToastContext';

const BASE = '/api/v1';

function authHeaders() {
  const token = localStorage.getItem('authToken');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function apiGet(path) {
  const res = await fetch(`${BASE}${path}`, { headers: authHeaders() });
  if (!res.ok) throw new Error(await res.text() || `HTTP ${res.status}`);
  return res.json();
}

async function apiPut(path, body) {
  const res = await fetch(`${BASE}${path}`, {
    method: 'PUT', headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(await res.text() || `HTTP ${res.status}`);
  return res.json();
}

async function apiDelete(path) {
  const res = await fetch(`${BASE}${path}`, { method: 'DELETE', headers: authHeaders() });
  if (!res.ok) throw new Error(await res.text() || `HTTP ${res.status}`);
}

async function apiUpload(path, file, params = {}) {
  const form = new FormData();
  form.append('file', file);
  const q = new URLSearchParams(params).toString();
  const res = await fetch(`${BASE}${path}${q ? '?' + q : ''}`, {
    method: 'POST', headers: authHeaders(), body: form,
  });
  if (!res.ok) throw new Error(await res.text() || `HTTP ${res.status}`);
  return res.json();
}

async function downloadFile(path, filename) {
  const res = await fetch(`${BASE}${path}`, { headers: authHeaders() });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a'); a.href = url; a.download = filename; a.click();
  URL.revokeObjectURL(url);
}

const thS = { padding: '7px 10px', textAlign: 'left', fontSize: 11, fontWeight: 700, color: 'var(--wpw-gray)', textTransform: 'uppercase', letterSpacing: '0.04em' };
const tdS = { padding: '8px 10px', fontSize: 13 };

function Stat({ label, value, color }) {
  return (
    <div>
      <div style={{ fontSize: 11, color: 'var(--wpw-mid-gray)' }}>{label}</div>
      <div style={{ fontSize: 18, fontWeight: 700, color: color || 'var(--wpw-navy)' }}>{value}</div>
    </div>
  );
}

function ImportPanel({ dealerId, onImported }) {
  const toast = useToast();
  const fileRef = useRef();
  const [file, setFile] = useState(null);
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(false);
  const base = `/admin/dealers/${dealerId}/sku-mapping`;

  async function handleValidate() {
    if (!file) { toast('Выберите файл', 'warning'); return; }
    setLoading(true);
    try { setReport(await apiUpload(`${base}/validate`, file)); }
    catch (e) { toast(e.message, 'error'); }
    finally { setLoading(false); }
  }

  async function handleExecute(skipGhosts) {
    setLoading(true);
    try {
      const r = await apiUpload(`${base}/execute`, file, { skipGhosts });
      toast(`Импортировано: ${r.imported} (создано ${r.created}, обновлено ${r.updated})`, 'success');
      setReport(null); setFile(null);
      if (fileRef.current) fileRef.current.value = '';
      onImported?.();
    } catch (e) { toast(e.message, 'error'); }
    finally { setLoading(false); }
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
        <input ref={fileRef} type="file" accept=".xlsx,.xls" style={{ fontSize: 12 }}
          onChange={e => { setFile(e.target.files[0] || null); setReport(null); }} />
        <button className="btn btn-primary" style={{ fontSize: 12 }} onClick={handleValidate} disabled={loading || !file}>
          {loading ? '…' : 'Проверить'}
        </button>
        <button className="btn btn-secondary" style={{ fontSize: 12 }}
          onClick={() => downloadFile(`${base}/template`, 'sku-mapping-template.xlsx')}>
          ⬇ Шаблон
        </button>
      </div>

      {report && (
        <div style={{ border: '1px solid var(--wpw-border)', borderRadius: 6, overflow: 'hidden' }}>
          <div style={{ padding: '10px 14px', background: '#f5f7fa', borderBottom: '1px solid var(--wpw-border)', display: 'flex', gap: 20, flexWrap: 'wrap' }}>
            <Stat label="Всего" value={report.total} />
            <Stat label="В каталоге" value={report.valid.length} color="#2e7d32" />
            <Stat label="Призраки" value={report.ghosts.length} color={report.ghosts.length > 0 ? '#e65100' : '#2e7d32'} />
          </div>

          {report.ghosts.length > 0 && (
            <div style={{ padding: '8px 14px', background: '#fff8e1', borderBottom: '1px solid var(--wpw-border)' }}>
              <div style={{ fontSize: 11, fontWeight: 600, color: '#e65100', marginBottom: 4 }}>Не найдены в каталоге:</div>
              <div style={{ fontSize: 11, fontFamily: 'monospace', display: 'flex', flexWrap: 'wrap', gap: 4 }}>
                {report.ghosts.map(g => (
                  <span key={g.wpwSku} style={{ background: '#ffe082', padding: '1px 5px', borderRadius: 3 }}>{g.wpwSku}</span>
                ))}
              </div>
            </div>
          )}

          {report.errors.length > 0 && (
            <div style={{ padding: '8px 14px', background: '#ffebee', borderBottom: '1px solid var(--wpw-border)' }}>
              {report.errors.map((e, i) => <div key={i} style={{ fontSize: 11, color: '#c62828' }}>• {e}</div>)}
            </div>
          )}

          <div style={{ padding: '10px 14px', display: 'flex', gap: 8 }}>
            <button className="btn btn-primary" style={{ fontSize: 12 }} onClick={() => handleExecute(true)} disabled={loading || report.valid.length === 0}>
              Без призраков ({report.valid.length})
            </button>
            <button className="btn btn-secondary" style={{ fontSize: 12 }} onClick={() => handleExecute(false)} disabled={loading}>
              Всё ({report.total - report.errors.length})
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

function MappingTable({ dealerId, rows, onRefresh }) {
  const toast = useToast();
  const base = `/admin/dealers/${dealerId}/sku-mapping`;
  const [editKey, setEditKey] = useState(null);
  const [editForm, setEditForm] = useState({});
  const [newRow, setNewRow] = useState({ wpwSku: '', dealerSku: '', dealerBrand: '' });
  const [adding, setAdding] = useState(false);
  const [saving, setSaving] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(null);

  async function saveEdit(wpwSku) {
    setSaving(true);
    try { await apiPut(base, { wpwSku, ...editForm }); setEditKey(null); onRefresh(); }
    catch (e) { toast(e.message, 'error'); }
    finally { setSaving(false); }
  }

  async function saveNew() {
    if (!newRow.wpwSku.trim() || !newRow.dealerSku.trim()) { toast('WPW SKU и Dealer SKU обязательны', 'warning'); return; }
    setSaving(true);
    try { await apiPut(base, newRow); setNewRow({ wpwSku: '', dealerSku: '', dealerBrand: '' }); setAdding(false); onRefresh(); }
    catch (e) { toast(e.message, 'error'); }
    finally { setSaving(false); }
  }

  async function handleDelete(wpwSku) {
    try { await apiDelete(`${base}/${encodeURIComponent(wpwSku)}`); onRefresh(); toast('Удалено', 'success'); }
    catch (e) { toast(e.message, 'error'); }
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
        <span style={{ fontSize: 12, color: 'var(--wpw-mid-gray)' }}>{rows.length} записей</span>
        <div style={{ display: 'flex', gap: 6 }}>
          <button className="btn btn-secondary" style={{ fontSize: 11, padding: '4px 8px' }}
            onClick={() => downloadFile(`${base}/export`, `sku-mapping-${dealerId}.xlsx`)}>⬇ Экспорт</button>
          {!adding && <button className="btn btn-secondary" style={{ fontSize: 11, padding: '4px 8px' }} onClick={() => setAdding(true)}>+ Строка</button>}
        </div>
      </div>
      <div style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
          <thead>
            <tr style={{ borderBottom: '2px solid #e8edf5' }}>
              {['WPW SKU', 'Dealer SKU', 'Бренд', ''].map(h => <th key={h} style={thS}>{h}</th>)}
            </tr>
          </thead>
          <tbody>
            {adding && (
              <tr style={{ background: '#f0f7ff' }}>
                <td style={tdS}><input className="input" style={{ width: '100%' }} placeholder="WPW-001" value={newRow.wpwSku} onChange={e => setNewRow(f => ({ ...f, wpwSku: e.target.value }))} autoFocus /></td>
                <td style={tdS}><input className="input" style={{ width: '100%' }} placeholder="MY-SKU" value={newRow.dealerSku} onChange={e => setNewRow(f => ({ ...f, dealerSku: e.target.value }))} /></td>
                <td style={tdS}><input className="input" style={{ width: '100%' }} placeholder="Бренд" value={newRow.dealerBrand} onChange={e => setNewRow(f => ({ ...f, dealerBrand: e.target.value }))} /></td>
                <td style={{ ...tdS, whiteSpace: 'nowrap' }}>
                  <button className="btn btn-primary" style={{ fontSize: 11, padding: '3px 8px' }} onClick={saveNew} disabled={saving}>Сохр.</button>
                  <button className="btn btn-secondary" style={{ fontSize: 11, padding: '3px 8px', marginLeft: 4 }} onClick={() => setAdding(false)}>Отмена</button>
                </td>
              </tr>
            )}
            {rows.map(row => (
              <tr key={row.wpwSku} style={{ borderBottom: '1px solid #f0f2f5' }}>
                <td style={{ ...tdS, fontFamily: 'monospace', fontSize: 12, color: 'var(--wpw-mid-gray)' }}>{row.wpwSku}</td>
                {editKey === row.wpwSku ? (
                  <>
                    <td style={tdS}><input className="input" style={{ width: '100%' }} value={editForm.dealerSku} onChange={e => setEditForm(f => ({ ...f, dealerSku: e.target.value }))} autoFocus /></td>
                    <td style={tdS}><input className="input" style={{ width: '100%' }} value={editForm.dealerBrand} onChange={e => setEditForm(f => ({ ...f, dealerBrand: e.target.value }))} /></td>
                    <td style={{ ...tdS, whiteSpace: 'nowrap' }}>
                      <button className="btn btn-primary" style={{ fontSize: 11, padding: '3px 8px' }} onClick={() => saveEdit(row.wpwSku)} disabled={saving}>Сохр.</button>
                      <button className="btn btn-secondary" style={{ fontSize: 11, padding: '3px 8px', marginLeft: 4 }} onClick={() => setEditKey(null)}>Отмена</button>
                    </td>
                  </>
                ) : (
                  <>
                    <td style={{ ...tdS, fontFamily: 'monospace', fontSize: 12 }}>{row.dealerSku}</td>
                    <td style={tdS}>{row.dealerBrand || '—'}</td>
                    <td style={{ ...tdS, whiteSpace: 'nowrap' }}>
                      {confirmDelete === row.wpwSku ? (
                        <span style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
                          <span style={{ fontSize: 11, color: '#c62828' }}>Удалить?</span>
                          <button className="btn btn-primary" style={{ fontSize: 11, padding: '2px 6px', background: '#c62828', borderColor: '#c62828' }} onClick={() => { handleDelete(row.wpwSku); setConfirmDelete(null); }}>Да</button>
                          <button className="btn btn-secondary" style={{ fontSize: 11, padding: '2px 6px' }} onClick={() => setConfirmDelete(null)}>Нет</button>
                        </span>
                      ) : (
                        <span style={{ display: 'flex', gap: 4 }}>
                          <button className="btn btn-secondary" style={{ fontSize: 11, padding: '3px 8px' }} onClick={() => { setEditKey(row.wpwSku); setEditForm({ dealerSku: row.dealerSku || '', dealerBrand: row.dealerBrand || '' }); }} disabled={editKey !== null}>Изм.</button>
                          <button className="btn btn-secondary" style={{ fontSize: 11, padding: '3px 8px', color: '#c62828' }} onClick={() => setConfirmDelete(row.wpwSku)} disabled={editKey !== null}>Удал.</button>
                        </span>
                      )}
                    </td>
                  </>
                )}
              </tr>
            ))}
            {rows.length === 0 && !adding && (
              <tr><td colSpan={4} style={{ padding: '24px 10px', textAlign: 'center', color: 'var(--wpw-mid-gray)', fontSize: 12 }}>Маппинги отсутствуют</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default function AdminSkuMappingPanel({ dealer, onClose }) {
  const toast = useToast();
  const [tab, setTab] = useState('mapping');
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => { load(); }, [dealer.id]);

  async function load() {
    setLoading(true);
    try { setRows(await apiGet(`/admin/dealers/${dealer.id}/sku-mapping`)); }
    catch (e) { toast(e.message, 'error'); }
    finally { setLoading(false); }
  }

  return (
    <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.45)', zIndex: 1000, display: 'flex', alignItems: 'flex-start', justifyContent: 'center', padding: '32px 16px', overflowY: 'auto' }}
      onClick={e => e.target === e.currentTarget && onClose()}>
      <div style={{ background: '#fff', borderRadius: 10, width: '100%', maxWidth: 760, boxShadow: '0 8px 32px rgba(0,0,0,0.18)', overflow: 'hidden' }}>
        {/* header */}
        <div style={{ padding: '14px 20px', background: 'var(--wpw-navy)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <span style={{ color: '#fff', fontWeight: 700, fontSize: 14 }}>SKU Mapping — </span>
            <span style={{ color: '#90caf9', fontSize: 14 }}>{dealer.companyName || dealer.name}</span>
          </div>
          <button type="button" onClick={onClose} style={{ background: 'none', border: 'none', color: '#fff', fontSize: 18, cursor: 'pointer', padding: '2px 6px' }}>✕</button>
        </div>

        {/* tabs */}
        <div style={{ padding: '12px 20px 0', borderBottom: '1px solid var(--wpw-border)', display: 'flex', gap: 8 }}>
          {[['mapping', 'Маппинг'], ['import', 'Импорт Excel']].map(([key, label]) => (
            <button key={key} className={`btn ${tab === key ? 'btn-primary' : ''}`} style={{ fontSize: 12, padding: '5px 12px' }} onClick={() => setTab(key)}>{label}</button>
          ))}
        </div>

        {/* body */}
        <div style={{ padding: '16px 20px', maxHeight: 'calc(85vh - 120px)', overflowY: 'auto' }}>
          {tab === 'mapping' && (
            loading
              ? <div style={{ textAlign: 'center', padding: 32, color: 'var(--wpw-mid-gray)' }}><div className="spinner" style={{ margin: '0 auto 10px' }} />Загрузка…</div>
              : <MappingTable dealerId={dealer.id} rows={rows} onRefresh={load} />
          )}
          {tab === 'import' && <ImportPanel dealerId={dealer.id} onImported={load} />}
        </div>
      </div>
    </div>
  );
}
