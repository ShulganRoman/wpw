import {useState, useEffect} from 'react';
import {getPriceList, getSkuMapping, addSkuMapping} from '../api/api';
import {LoadingSpinner, ErrorState} from '../components/LoadingState';
import {useToast} from '../components/ToastContext';

function PriceListTab({apiKey}) {
    const toast = useToast();
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (!apiKey) return;

        async function fetchData() {
            setLoading(true);
            setError(null);
            try {
                const res = await getPriceList(apiKey);
                setData(Array.isArray(res) ? res : res.items || res.priceList || res.data || []);
            } catch (err) {
                setError(err.message);
                toast(err.message, 'error');
            } finally {
                setLoading(false);
            }
        }

        fetchData();
    }, [apiKey]);

    if (loading) return <LoadingSpinner text="Loading price list…"/>;
    if (error) return <ErrorState message={error}/>;

    if (data.length === 0) {
        return (
            <div className="empty-state">
                <div className="empty-state-icon">💰</div>
                <h3>No price data available</h3>
                <p>The price list is empty for this account.</p>
            </div>
        );
    }

    const headers = Object.keys(data[0] || {});

    return (
        <div style={{overflowX: 'auto'}}>
            <table className="data-table">
                <thead>
                <tr>
                    {headers.map(h => <th key={h}>{h}</th>)}
                </tr>
                </thead>
                <tbody>
                {data.map((row, i) => (
                    <tr key={i}>
                        {headers.map(h => (
                            <td key={h}>
                                {row[h] !== null && row[h] !== undefined ? String(row[h]) : '—'}
                            </td>
                        ))}
                    </tr>
                ))}
                </tbody>
            </table>
        </div>
    );
}

function SkuMappingTab({apiKey}) {
    const toast = useToast();
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [form, setForm] = useState({toolNo: '', dealerSku: '', note: ''});
    const [adding, setAdding] = useState(false);

    useEffect(() => {
        if (!apiKey) return;
        fetchData();
    }, [apiKey]);

    async function fetchData() {
        setLoading(true);
        setError(null);
        try {
            const res = await getSkuMapping(apiKey);
            setData(Array.isArray(res) ? res : res.items || res.mappings || res.data || []);
        } catch (err) {
            setError(err.message);
            toast(err.message, 'error');
        } finally {
            setLoading(false);
        }
    }

    async function handleAdd(e) {
        e.preventDefault();
        if (!form.toolNo.trim() || !form.dealerSku.trim()) {
            toast('Tool No and Dealer SKU are required.', 'warning');
            return;
        }
        setAdding(true);
        try {
            await addSkuMapping(apiKey, form.toolNo, form.dealerSku, form.note);
            toast('SKU mapping added successfully.', 'success');
            setForm({toolNo: '', dealerSku: '', note: ''});
            fetchData();
        } catch (err) {
            toast(err.message, 'error');
        } finally {
            setAdding(false);
        }
    }

    if (loading) return <LoadingSpinner text="Loading SKU mappings…"/>;
    if (error) return <ErrorState message={error} onRetry={fetchData}/>;

    return (
        <div>
            {data.length > 0 ? (
                <div style={{overflowX: 'auto'}}>
                    <table className="data-table">
                        <thead>
                        <tr>
                            <th>Tool No</th>
                            <th>Dealer SKU</th>
                            <th>Note</th>
                        </tr>
                        </thead>
                        <tbody>
                        {data.map((row, i) => (
                            <tr key={i}>
                                <td style={{fontFamily: 'var(--wpw-font-mono)', fontSize: 12}}>
                                    {row.toolNo || row.tool_no || '—'}
                                </td>
                                <td style={{fontFamily: 'var(--wpw-font-mono)', fontSize: 12}}>
                                    {row.dealerSku || row.dealer_sku || '—'}
                                </td>
                                <td>{row.note || '—'}</td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            ) : (
                <div className="empty-state">
                    <div className="empty-state-icon">🗂️</div>
                    <h3>No SKU mappings yet</h3>
                    <p>Add your first mapping using the form below.</p>
                </div>
            )}

            <form className="add-sku-form" onSubmit={handleAdd}>
                <div className="form-group">
                    <label className="form-label">Tool No *</label>
                    <input
                        className="form-control"
                        type="text"
                        placeholder="e.g. T-123"
                        value={form.toolNo}
                        onChange={e => setForm(f => ({...f, toolNo: e.target.value}))}
                    />
                </div>
                <div className="form-group">
                    <label className="form-label">Dealer SKU *</label>
                    <input
                        className="form-control"
                        type="text"
                        placeholder="e.g. MY-SKU-001"
                        value={form.dealerSku}
                        onChange={e => setForm(f => ({...f, dealerSku: e.target.value}))}
                    />
                </div>
                <div className="form-group">
                    <label className="form-label">Note</label>
                    <input
                        className="form-control"
                        type="text"
                        placeholder="Optional note"
                        value={form.note}
                        onChange={e => setForm(f => ({...f, note: e.target.value}))}
                    />
                </div>
                <div className="form-group">
                    <label className="form-label" style={{visibility: 'hidden'}}>Add</label>
                    <button type="submit" className="btn btn-primary" disabled={adding}>
                        {adding ? 'Adding…' : '+ Add Mapping'}
                    </button>
                </div>
            </form>
        </div>
    );
}

export default function DealerPage() {
    const toast = useToast();
    const [apiKey, setApiKey] = useState(() => sessionStorage.getItem('dealer_api_key') || '');
    const [inputKey, setInputKey] = useState(apiKey);
    const [authenticated, setAuthenticated] = useState(!!apiKey);
    const [activeTab, setActiveTab] = useState('price-list');
    const [checking, setChecking] = useState(false);

    async function handleAuth(e) {
        e.preventDefault();
        if (!inputKey.trim()) {
            toast('Please enter an API key.', 'warning');
            return;
        }
        setChecking(true);
        try {
            await getPriceList(inputKey.trim());
            const key = inputKey.trim();
            setApiKey(key);
            sessionStorage.setItem('dealer_api_key', key);
            setAuthenticated(true);
            toast('Authenticated successfully.', 'success');
        } catch (err) {
            if (err.message.includes('401') || err.message.includes('403') || err.message.toLowerCase().includes('unauthorized')) {
                toast('Invalid API key. Please try again.', 'error');
            } else {
                // Accept anyway - might just be a network issue
                const key = inputKey.trim();
                setApiKey(key);
                sessionStorage.setItem('dealer_api_key', key);
                setAuthenticated(true);
            }
        } finally {
            setChecking(false);
        }
    }

    function handleLogout() {
        setApiKey('');
        setInputKey('');
        setAuthenticated(false);
        sessionStorage.removeItem('dealer_api_key');
        toast('Logged out.', 'info');
    }

    if (!authenticated) {
        return (
            <div>
                <div className="page-header">
                    <h1 className="page-title">Dealer Portal</h1>
                    <p className="page-subtitle">Enter your API key to access dealer pricing and SKU data</p>
                </div>

                <div className="card" style={{maxWidth: 440}}>
                    <div className="card-title">Authentication</div>
                    <form onSubmit={handleAuth} style={{display: 'flex', flexDirection: 'column', gap: 16}}>
                        <div className="form-group">
                            <label className="form-label" htmlFor="api-key">API Key</label>
                            <input
                                id="api-key"
                                className="form-control"
                                type="password"
                                placeholder="Enter your dealer API key…"
                                value={inputKey}
                                onChange={e => setInputKey(e.target.value)}
                                autoFocus
                                style={{fontFamily: 'var(--wpw-font-mono)'}}
                            />
                        </div>
                        <button type="submit" className="btn btn-primary" disabled={checking}>
                            {checking ? (
                                <>
                                    <div className="spinner" style={{width: 14, height: 14, borderWidth: 2}}/>
                                    Verifying…
                                </>
                            ) : '🔑 Access Dealer Portal'}
                        </button>
                    </form>
                </div>
            </div>
        );
    }

    return (
        <div>
            <div className="page-header"
                 style={{display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between'}}>
                <div>
                    <h1 className="page-title">Dealer Portal</h1>
                    <p className="page-subtitle">
                        Authenticated with key: <code style={{fontFamily: 'var(--wpw-font-mono)', fontSize: 12}}>
                        {apiKey.slice(0, 4)}{'*'.repeat(Math.max(0, apiKey.length - 4))}
                    </code>
                    </p>
                </div>
                <button className="btn btn-secondary" onClick={handleLogout}>
                    Log out
                </button>
            </div>

            <div className="dealer-tabs">
                <button
                    className={`dealer-tab${activeTab === 'price-list' ? ' active' : ''}`}
                    onClick={() => setActiveTab('price-list')}
                >
                    💰 Price List
                </button>
                <button
                    className={`dealer-tab${activeTab === 'sku-mapping' ? ' active' : ''}`}
                    onClick={() => setActiveTab('sku-mapping')}
                >
                    🗂️ SKU Mapping
                </button>
            </div>

            {activeTab === 'price-list' && <PriceListTab apiKey={apiKey}/>}
            {activeTab === 'sku-mapping' && <SkuMappingTab apiKey={apiKey}/>}
        </div>
    );
}
