import { useState, useEffect, useRef } from 'react';
import { useToast } from './ToastContext';
import {
  getStockPrices, upsertStockPrice, deleteStockPrice,
  importStockPrices, downloadStockPrices, downloadStockPriceTemplate,
} from '../api/api';

async function downloadFile(fetchFn, fallbackName) {
  const res = await fetchFn();
  const blob = await res.blob();
  const disp = res.headers.get('content-disposition') || '';
  const match = disp.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
  const name = match ? match[1].replace(/['"]/g, '') : fallbackName;
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url; a.download = name; a.click();
  URL.revokeObjectURL(url);
}

function AddRowForm({ onSave }) {
  const [toolNo, setToolNo] = useState('');
  const [minQty, setMinQty] = useState(1);
  const [price, setPrice] = useState('');

  function handleSubmit(e) {
    e.preventDefault();
    if (!toolNo.trim() || !price) return;
    onSave({ toolNo: toolNo.trim(), minQty: Number(minQty), price: Number(price) });
    setToolNo(''); setMinQty(1); setPrice('');
  }

  return (
    <form onSubmit={handleSubmit} style={{ display: 'flex', gap: 8, marginBottom: 16, alignItems: 'flex-end', flexWrap: 'wrap' }}>
      <div>
        <div style={{ fontSize: 11, color: 'var(--wpw-gray)', marginBottom: 4 }}>Tool No</div>
        <input className="form-control" value={toolNo} onChange={e => setToolNo(e.target.value)} placeholder="WPW-001" style={{ width: 140 }} required />
      </div>
      <div>
        <div style={{ fontSize: 11, color: 'var(--wpw-gray)', marginBottom: 4 }}>Min Qty</div>
        <input className="form-control" type="number" min={1} value={minQty} onChange={e => setMinQty(e.target.value)} style={{ width: 80 }} required />
      </div>
      <div>
        <div style={{ fontSize: 11, color: 'var(--wpw-gray)', marginBottom: 4 }}>Price (USD)</div>
        <input className="form-control" type="number" min={0} step="0.01" value={price} onChange={e => setPrice(e.target.value)} placeholder="0.00" style={{ width: 100 }} required />
      </div>
      <button className="btn btn-primary" type="submit" style={{ height: 36 }}>+ Add</button>
    </form>
  );
}

function PriceRow({ item, onSave, onDelete }) {
  const [editing, setEditing] = useState(false);
  const [price, setPrice] = useState(String(item.price));

  function handleSave() {
    onSave({ toolNo: item.toolNo, minQty: item.minQty, price: Number(price) });
    setEditing(false);
  }

  return (
    <tr>
      <td style={{ padding: '6px 8px', fontSize: 13 }}>{item.toolNo}</td>
      <td style={{ padding: '6px 8px', fontSize: 13, textAlign: 'center' }}>{item.minQty}</td>
      <td style={{ padding: '6px 8px', fontSize: 13 }}>
        {editing ? (
          <input className="form-control" type="number" min={0} step="0.01" value={price}
            onChange={e => setPrice(e.target.value)} style={{ width: 100 }} autoFocus />
        ) : (
          <span style={{ fontWeight: 600, color: 'var(--wpw-primary)' }}>${Number(item.price).toFixed(2)}</span>
        )}
      </td>
      <td style={{ padding: '6px 8px', textAlign: 'right' }}>
        {editing ? (
          <div style={{ display: 'flex', gap: 4, justifyContent: 'flex-end' }}>
            <button className="btn btn-primary btn-sm" onClick={handleSave}>Save</button>
            <button className="btn btn-sm" onClick={() => { setEditing(false); setPrice(String(item.price)); }}>Cancel</button>
          </div>
        ) : (
          <div style={{ display: 'flex', gap: 4, justifyContent: 'flex-end' }}>
            <button className="btn btn-sm" onClick={() => setEditing(true)}>Edit</button>
            <button className="btn btn-sm" style={{ color: '#c62828' }} onClick={() => onDelete(item.toolNo, item.minQty)}>Delete</button>
          </div>
        )}
      </td>
    </tr>
  );
}

function ManualTab() {
  const toast = useToast();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  useEffect(() => {
    getStockPrices().then(setItems).catch(e => toast.error(e.message)).finally(() => setLoading(false));
  }, []);

  async function handleSave(data) {
    try {
      const updated = await upsertStockPrice(data);
      setItems(prev => {
        const idx = prev.findIndex(i => i.toolNo === updated.toolNo && i.minQty === updated.minQty);
        return idx >= 0 ? prev.map((i, n) => n === idx ? updated : i) : [...prev, updated];
      });
      toast.success('Saved');
    } catch (e) { toast.error(e.message); }
  }

  async function handleDelete(toolNo, minQty) {
    if (!confirm(`Delete price for ${toolNo} (min qty ${minQty})?`)) return;
    try {
      await deleteStockPrice(toolNo, minQty);
      setItems(prev => prev.filter(i => !(i.toolNo === toolNo && i.minQty === minQty)));
      toast.success('Deleted');
    } catch (e) { toast.error(e.message); }
  }

  const filtered = items.filter(i => i.toolNo.toLowerCase().includes(search.toLowerCase()));

  return (
    <div>
      <AddRowForm onSave={handleSave} />
      <div style={{ display: 'flex', gap: 8, marginBottom: 12, alignItems: 'center' }}>
        <input className="form-control" placeholder="Search by tool no…" value={search}
          onChange={e => setSearch(e.target.value)} style={{ width: 220 }} />
        <span style={{ fontSize: 12, color: 'var(--wpw-gray)' }}>{filtered.length} rows</span>
      </div>
      {loading ? <p style={{ color: 'var(--wpw-gray)', fontSize: 13 }}>Loading…</p> : (
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ borderBottom: '2px solid var(--wpw-border)' }}>
              {['Tool No', 'Min Qty', 'Price (USD)', ''].map(h => (
                <th key={h} style={{ padding: '6px 8px', textAlign: h === '' ? 'right' : 'left', fontSize: 12, color: 'var(--wpw-gray)', fontWeight: 600 }}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {filtered.map(item => (
              <PriceRow key={`${item.toolNo}-${item.minQty}`} item={item} onSave={handleSave} onDelete={handleDelete} />
            ))}
            {filtered.length === 0 && (
              <tr><td colSpan={4} style={{ padding: 24, textAlign: 'center', color: 'var(--wpw-gray)', fontSize: 13 }}>No prices yet</td></tr>
            )}
          </tbody>
        </table>
      )}
    </div>
  );
}

function ImportTab() {
  const toast = useToast();
  const fileRef = useRef(null);
  const [file, setFile] = useState(null);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  async function handleImport() {
    if (!file) return;
    setLoading(true);
    try {
      const res = await importStockPrices(file);
      setResult(res);
      if (res.errors.length === 0) toast.success(`Imported ${res.imported} prices`);
      else toast.warn(`Imported ${res.imported}, skipped ${res.skipped}`);
    } catch (e) { toast.error(e.message); }
    finally { setLoading(false); }
  }

  return (
    <div>
      <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap' }}>
        <button className="btn" onClick={() => downloadFile(downloadStockPriceTemplate, 'stock-prices-template.xlsx')}>
          ↓ Template
        </button>
        <button className="btn" onClick={() => downloadFile(downloadStockPrices, 'stock-prices.xlsx')}>
          ↓ Export current
        </button>
      </div>
      <div style={{ marginBottom: 12 }}>
        <input type="file" accept=".xlsx,.xls" ref={fileRef} style={{ display: 'none' }}
          onChange={e => { setFile(e.target.files[0]); setResult(null); }} />
        <button className="btn" onClick={() => fileRef.current.click()}>
          {file ? `📄 ${file.name}` : 'Choose Excel file'}
        </button>
      </div>
      {file && (
        <button className="btn btn-primary" onClick={handleImport} disabled={loading}>
          {loading ? 'Importing…' : 'Import (replace all)'}
        </button>
      )}
      {result && (
        <div style={{ marginTop: 16, padding: 12, background: result.errors.length ? '#fff8e1' : '#e8f5e9', borderRadius: 6, fontSize: 13 }}>
          <div><strong>Imported:</strong> {result.imported} | <strong>Skipped:</strong> {result.skipped}</div>
          {result.errors.length > 0 && (
            <ul style={{ marginTop: 8, paddingLeft: 20, color: '#c62828' }}>
              {result.errors.map((e, i) => <li key={i}>{e}</li>)}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}

export default function StockPricePanel() {
  const [tab, setTab] = useState('manual');

  return (
    <div className="admin-section">
      <h2 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>Stock Prices (USD)</h2>
      <div style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
        <button className={`btn btn-sm ${tab === 'manual' ? 'btn-primary' : ''}`} onClick={() => setTab('manual')}>Manual</button>
        <button className={`btn btn-sm ${tab === 'import' ? 'btn-primary' : ''}`} onClick={() => setTab('import')}>Excel Import</button>
      </div>
      {tab === 'manual' && <ManualTab />}
      {tab === 'import' && <ImportTab />}
    </div>
  );
}
