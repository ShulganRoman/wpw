import { useState, useEffect, useRef } from 'react';
import { useToast } from './ToastContext';
import { getDealerPriceList, importDealerPriceList, deleteDealerPriceList, downloadDealerPriceList, downloadDealerPriceTemplate, getCurrencies } from '../api/api';

async function downloadFile(fetchFn, fallbackName) {
  const res = await fetchFn();
  const blob = await res.blob();
  const disp = res.headers.get('content-disposition') || '';
  const match = disp.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
  const name = match ? match[1].replace(/['"]/g, '') : fallbackName;
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a'); a.href = url; a.download = name; a.click();
  URL.revokeObjectURL(url);
}

export default function AdminDealerPricePanel({ dealer, onClose }) {
  const toast = useToast();
  const fileRef = useRef(null);
  const [priceList, setPriceList] = useState(undefined); // undefined = loading, null = none
  const [currencies, setCurrencies] = useState([]);
  const [file, setFile] = useState(null);
  const [currencyCode, setCurrencyCode] = useState('USD');
  const [validTo, setValidTo] = useState('');
  const [importing, setImporting] = useState(false);
  const [result, setResult] = useState(null);

  useEffect(() => {
    getCurrencies().then(setCurrencies).catch(() => {});
    getDealerPriceList(dealer.id)
      .then(d => setPriceList(d))
      .catch(e => {
        if (e.message.includes('204') || e.message.includes('No Content')) setPriceList(null);
        else { toast.error(e.message); setPriceList(null); }
      });
  }, [dealer.id]);

  async function handleImport() {
    if (!file) return;
    setImporting(true);
    try {
      const res = await importDealerPriceList(dealer.id, file, currencyCode, validTo || null);
      setResult(res);
      const updated = await getDealerPriceList(dealer.id).catch(() => null);
      setPriceList(updated);
      toast.success(`Imported ${res.imported} prices`);
    } catch (e) { toast.error(e.message); }
    finally { setImporting(false); }
  }

  async function handleDelete() {
    if (!confirm('Delete price list for this dealer?')) return;
    try {
      await deleteDealerPriceList(dealer.id);
      setPriceList(null);
      setResult(null);
      toast.success('Price list deleted');
    } catch (e) { toast.error(e.message); }
  }

  return (
    <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <div style={{ background: '#fff', borderRadius: 10, width: '100%', maxWidth: 700, maxHeight: '90vh', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
        <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--wpw-border)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div>
            <strong style={{ fontSize: 15 }}>Price List — {dealer.companyName || dealer.name}</strong>
            {dealer.dealerCode && <span style={{ marginLeft: 8, fontSize: 12, color: 'var(--wpw-gray)' }}>@{dealer.dealerCode}</span>}
          </div>
          <button className="btn btn-sm" onClick={onClose}>✕</button>
        </div>

        <div style={{ padding: 20, overflowY: 'auto', flex: 1 }}>
          {/* Current price list info */}
          {priceList === undefined && <p style={{ color: 'var(--wpw-gray)', fontSize: 13 }}>Loading…</p>}
          {priceList === null && <p style={{ color: 'var(--wpw-gray)', fontSize: 13 }}>No price list assigned to this dealer.</p>}
          {priceList && (
            <div style={{ marginBottom: 20, padding: 12, background: priceList.expired ? '#fff8e1' : '#f5f5f5', borderRadius: 6, border: priceList.expired ? '1px solid #ffe082' : '1px solid var(--wpw-border)' }}>
              <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap', marginBottom: 8 }}>
                <span style={{ fontSize: 13 }}><strong>Currency:</strong> {priceList.currencyCode} {priceList.currencySymbol}</span>
                <span style={{ fontSize: 13 }}><strong>Valid from:</strong> {priceList.validFrom || '—'}</span>
                <span style={{ fontSize: 13 }}><strong>Valid to:</strong> {priceList.validTo || '—'}</span>
                <span style={{ fontSize: 13 }}><strong>Items:</strong> {priceList.items.length}</span>
              </div>
              {priceList.expired && (
                <div style={{ color: '#e65100', fontWeight: 600, fontSize: 13 }}>⚠ Price list is expired</div>
              )}
              <div style={{ display: 'flex', gap: 8, marginTop: 10 }}>
                <button className="btn btn-sm" onClick={() => downloadFile(() => downloadDealerPriceList(dealer.id), 'dealer-price-list.xlsx')}>↓ Export</button>
                <button className="btn btn-sm" style={{ color: '#c62828' }} onClick={handleDelete}>Delete</button>
              </div>
              {priceList.items.length > 0 && (
                <div style={{ marginTop: 12, maxHeight: 240, overflowY: 'auto' }}>
                  <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
                    <thead>
                      <tr style={{ borderBottom: '1px solid var(--wpw-border)' }}>
                        <th style={{ padding: '4px 8px', textAlign: 'left' }}>Tool No</th>
                        <th style={{ padding: '4px 8px', textAlign: 'center' }}>Min Qty</th>
                        <th style={{ padding: '4px 8px', textAlign: 'right' }}>Price</th>
                      </tr>
                    </thead>
                    <tbody>
                      {priceList.items.map((item, i) => (
                        <tr key={i} style={{ borderBottom: '1px solid #f0f0f0' }}>
                          <td style={{ padding: '4px 8px' }}>{item.toolNo}</td>
                          <td style={{ padding: '4px 8px', textAlign: 'center' }}>{item.minQty}</td>
                          <td style={{ padding: '4px 8px', textAlign: 'right', fontWeight: 600, color: 'var(--wpw-primary)' }}>
                            {priceList.currencySymbol}{Number(item.price).toFixed(2)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {/* Import section */}
          <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 12 }}>Import new price list</h3>
          <div style={{ display: 'flex', gap: 8, marginBottom: 12, flexWrap: 'wrap' }}>
            <button className="btn btn-sm" onClick={() => downloadFile(downloadDealerPriceTemplate, 'price-list-template.xlsx')}>↓ Template</button>
          </div>
          <div style={{ display: 'flex', gap: 12, marginBottom: 12, flexWrap: 'wrap', alignItems: 'flex-end' }}>
            <div>
              <div style={{ fontSize: 11, color: 'var(--wpw-gray)', marginBottom: 4 }}>Currency</div>
              <select className="form-control" value={currencyCode} onChange={e => setCurrencyCode(e.target.value)} style={{ width: 100 }}>
                {currencies.map(c => <option key={c.code} value={c.code}>{c.code} {c.symbol}</option>)}
              </select>
            </div>
            <div>
              <div style={{ fontSize: 11, color: 'var(--wpw-gray)', marginBottom: 4 }}>Valid to (optional)</div>
              <input className="form-control" type="date" value={validTo} onChange={e => setValidTo(e.target.value)} style={{ width: 150 }} />
            </div>
          </div>
          <div style={{ display: 'flex', gap: 8, marginBottom: 12, alignItems: 'center' }}>
            <input type="file" accept=".xlsx,.xls" ref={fileRef} style={{ display: 'none' }}
              onChange={e => { setFile(e.target.files[0]); setResult(null); }} />
            <button className="btn" onClick={() => fileRef.current.click()}>
              {file ? `📄 ${file.name}` : 'Choose Excel file'}
            </button>
            {file && (
              <button className="btn btn-primary" onClick={handleImport} disabled={importing}>
                {importing ? 'Importing…' : 'Import (replace all)'}
              </button>
            )}
          </div>
          {result && (
            <div style={{ padding: 12, background: result.errors.length ? '#fff8e1' : '#e8f5e9', borderRadius: 6, fontSize: 13 }}>
              <div><strong>Imported:</strong> {result.imported} | <strong>Skipped:</strong> {result.skipped}</div>
              {result.errors.length > 0 && (
                <ul style={{ marginTop: 8, paddingLeft: 20, color: '#c62828' }}>
                  {result.errors.map((e, i) => <li key={i}>{e}</li>)}
                </ul>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
