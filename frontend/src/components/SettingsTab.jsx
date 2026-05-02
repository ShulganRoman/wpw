import { useState, useEffect, useCallback } from 'react';
import { getSystemSettings, updateSystemSettings, getSystemStats, deleteAllProductMedia, getNotificationEmails, createNotificationEmail, updateNotificationEmail, deleteNotificationEmail } from '../api/api';
import { useToast } from './ToastContext';

function StatCard({ label, value, sub }) {
  return (
    <div style={{
      background: 'var(--wpw-surface)',
      border: '1px solid var(--wpw-border)',
      borderRadius: 8,
      padding: '12px 16px',
      minWidth: 160,
    }}>
      <div style={{ fontSize: 11, color: 'var(--wpw-text-muted)', marginBottom: 4, textTransform: 'uppercase', letterSpacing: '0.5px' }}>{label}</div>
      <div style={{ fontSize: 22, fontWeight: 700, color: 'var(--wpw-text)' }}>{value}</div>
      {sub && <div style={{ fontSize: 12, color: 'var(--wpw-text-muted)', marginTop: 2 }}>{sub}</div>}
    </div>
  );
}

function Toggle({ label, description, checked, onChange, disabled }) {
  return (
    <label style={{ display: 'flex', alignItems: 'flex-start', gap: 12, cursor: disabled ? 'not-allowed' : 'pointer', opacity: disabled ? 0.6 : 1 }}>
      <div style={{ position: 'relative', flexShrink: 0, marginTop: 2 }}>
        <input
          type="checkbox"
          checked={checked}
          onChange={e => onChange(e.target.checked)}
          disabled={disabled}
          style={{ position: 'absolute', opacity: 0, width: 0, height: 0 }}
        />
        <div
          onClick={() => !disabled && onChange(!checked)}
          style={{
            width: 42, height: 24, borderRadius: 12,
            background: checked ? 'var(--wpw-primary, #2563eb)' : 'var(--wpw-border)',
            transition: 'background 0.2s',
            cursor: disabled ? 'not-allowed' : 'pointer',
            position: 'relative',
          }}
        >
          <div style={{
            position: 'absolute', top: 3, left: checked ? 21 : 3,
            width: 18, height: 18, borderRadius: '50%', background: '#fff',
            transition: 'left 0.2s',
            boxShadow: '0 1px 3px rgba(0,0,0,0.2)',
          }} />
        </div>
      </div>
      <div>
        <div style={{ fontWeight: 500, fontSize: 14 }}>{label}</div>
        {description && <div style={{ fontSize: 12, color: 'var(--wpw-text-muted)', marginTop: 2 }}>{description}</div>}
      </div>
    </label>
  );
}

function Section({ title, children }) {
  return (
    <div style={{ marginBottom: 32 }}>
      <h3 style={{ margin: '0 0 16px', fontSize: 16, fontWeight: 600, borderBottom: '1px solid var(--wpw-border)', paddingBottom: 8 }}>
        {title}
      </h3>
      {children}
    </div>
  );
}

function pct(val) {
  return `${val.toFixed(1)}%`;
}

function NotificationEmailsSection() {
  const { showToast } = useToast();
  const [emails, setEmails] = useState([]);
  const [loading, setLoading] = useState(true);
  const [newEmail, setNewEmail] = useState('');
  const [adding, setAdding] = useState(false);
  const [editId, setEditId] = useState(null);
  const [editValue, setEditValue] = useState('');

  useEffect(() => {
    getNotificationEmails()
      .then(setEmails)
      .catch(() => showToast('Failed to load addresses', 'error'))
      .finally(() => setLoading(false));
  }, []);

  async function handleAdd() {
    if (!newEmail.trim()) return;
    setAdding(true);
    try {
      const created = await createNotificationEmail({ email: newEmail.trim(), active: true });
      setEmails(prev => [...prev, created]);
      setNewEmail('');
      showToast('Address added', 'success');
    } catch (e) {
      showToast(e.message || 'Error adding address', 'error');
    } finally {
      setAdding(false);
    }
  }

  async function handleToggleActive(entry) {
    try {
      const updated = await updateNotificationEmail(entry.id, { email: entry.email, active: !entry.active });
      setEmails(prev => prev.map(e => e.id === updated.id ? updated : e));
    } catch {
      showToast('Error updating address', 'error');
    }
  }

  async function handleSaveEdit(entry) {
    if (!editValue.trim()) return;
    try {
      const updated = await updateNotificationEmail(entry.id, { email: editValue.trim(), active: entry.active });
      setEmails(prev => prev.map(e => e.id === updated.id ? updated : e));
      setEditId(null);
      showToast('Address updated', 'success');
    } catch (e) {
      showToast(e.message || 'Error updating address', 'error');
    }
  }

  async function handleDelete(id) {
    try {
      await deleteNotificationEmail(id);
      setEmails(prev => prev.filter(e => e.id !== id));
      showToast('Address deleted', 'success');
    } catch {
      showToast('Error deleting address', 'error');
    }
  }

  return (
    <div>
      <p style={{ fontSize: 13, color: 'var(--wpw-text-muted)', margin: '0 0 16px' }}>
        These addresses will receive notifications about new dealer orders.
      </p>

      {loading ? (
        <div style={{ color: '#888', fontSize: 13 }}>Loading...</div>
      ) : (
        <>
          {emails.length > 0 && (
            <div style={{ marginBottom: 16, display: 'flex', flexDirection: 'column', gap: 8 }}>
              {emails.map(entry => (
                <div key={entry.id} style={{
                  display: 'flex', alignItems: 'center', gap: 8,
                  padding: '8px 12px', border: '1px solid var(--wpw-border)',
                  borderRadius: 6, background: entry.active ? '#fff' : '#fafafa',
                }}>
                  {editId === entry.id ? (
                    <>
                      <input
                        className="input"
                        value={editValue}
                        onChange={e => setEditValue(e.target.value)}
                        style={{ flex: 1, padding: '4px 8px', fontSize: 13 }}
                        onKeyDown={e => { if (e.key === 'Enter') handleSaveEdit(entry); if (e.key === 'Escape') setEditId(null); }}
                        autoFocus
                      />
                      <button className="btn btn-primary" style={{ padding: '3px 10px', fontSize: 12 }} onClick={() => handleSaveEdit(entry)}>Save</button>
                      <button className="btn" style={{ padding: '3px 10px', fontSize: 12 }} onClick={() => setEditId(null)}>Cancel</button>
                    </>
                  ) : (
                    <>
                      <span style={{ flex: 1, fontSize: 13, color: entry.active ? 'inherit' : '#aaa', textDecoration: entry.active ? 'none' : 'line-through' }}>
                        {entry.email}
                      </span>
                      <button
                        className="btn btn-secondary"
                        style={{ padding: '2px 8px', fontSize: 11, color: entry.active ? '#2e7d32' : '#888' }}
                        onClick={() => handleToggleActive(entry)}
                        title={entry.active ? 'Deactivate' : 'Activate'}
                      >
                        {entry.active ? 'Active' : 'Off'}
                      </button>
                      <button
                        className="btn btn-secondary"
                        style={{ padding: '2px 8px', fontSize: 11 }}
                        onClick={() => { setEditId(entry.id); setEditValue(entry.email); }}
                      >
                        Edit
                      </button>
                      <button
                        className="btn btn-secondary"
                        style={{ padding: '2px 8px', fontSize: 11, color: '#c62828' }}
                        onClick={() => handleDelete(entry.id)}
                      >
                        Delete
                      </button>
                    </>
                  )}
                </div>
              ))}
            </div>
          )}

          <div style={{ display: 'flex', gap: 8 }}>
            <input
              className="input"
              type="email"
              value={newEmail}
              onChange={e => setNewEmail(e.target.value)}
              placeholder="email@example.com"
              style={{ flex: 1, padding: '6px 10px', fontSize: 13 }}
              onKeyDown={e => e.key === 'Enter' && handleAdd()}
            />
            <button className="btn btn-primary" onClick={handleAdd} disabled={adding || !newEmail.trim()} style={{ fontSize: 13 }}>
              {adding ? 'Adding...' : 'Add'}
            </button>
          </div>
        </>
      )}
    </div>
  );
}

export default function SettingsTab() {
  const { showToast } = useToast();

  const [settings, setSettings] = useState(null);
  const [saving, setSaving] = useState(false);

  const [stats, setStats] = useState(null);
  const [statsLoading, setStatsLoading] = useState(false);

  const [deleteConfirm, setDeleteConfirm] = useState(false);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    getSystemSettings().then(setSettings).catch(() => showToast('Failed to load settings', 'error'));
    loadStats();
  }, []);

  const loadStats = useCallback(() => {
    setStatsLoading(true);
    getSystemStats()
      .then(setStats)
      .catch(() => showToast('Failed to load statistics', 'error'))
      .finally(() => setStatsLoading(false));
  }, []);

  const handleToggle = async (field, value) => {
    const next = { ...settings, [field]: value };
    setSettings(next);
    setSaving(true);
    try {
      await updateSystemSettings(next);
      showToast('Settings saved', 'success');
    } catch {
      showToast('Error saving settings', 'error');
      setSettings(settings); // rollback
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteAll = async () => {
    setDeleting(true);
    try {
      const result = await deleteAllProductMedia();
      showToast(`Deleted: ${result.deletedRecords} records, ${result.deletedDirectories} folders`, 'success');
      setDeleteConfirm(false);
      loadStats();
    } catch {
      showToast('Error deleting media', 'error');
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div style={{ maxWidth: 900 }}>
      {/* --- Visibility --- */}
      <Section title="Product Visibility">
        <p style={{ fontSize: 13, color: 'var(--wpw-text-muted)', margin: '0 0 16px' }}>
          When enabled — only products with their own images (not inherited from group/category) are shown in the corresponding section.
        </p>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <Toggle
            label="Photos only — for admins"
            description="Applies to the admin panel and API with ADMIN/MANAGE_* permissions"
            checked={settings?.requireImagesAdmin ?? false}
            onChange={v => handleToggle('requireImagesAdmin', v)}
            disabled={!settings || saving}
          />
          <Toggle
            label="Photos only — for dealers"
            description="Applies to the dealer account (DEALER role)"
            checked={settings?.requireImagesDealer ?? false}
            onChange={v => handleToggle('requireImagesDealer', v)}
            disabled={!settings || saving}
          />
          <Toggle
            label="Photos only — for public"
            description="Applies to the public catalog and search"
            checked={settings?.requireImagesPublic ?? false}
            onChange={v => handleToggle('requireImagesPublic', v)}
            disabled={!settings || saving}
          />
        </div>
      </Section>

      {/* --- Price visibility --- */}
      <Section title="Price Visibility">
        <p style={{ fontSize: 13, color: 'var(--wpw-text-muted)', margin: '0 0 16px' }}>
          When enabled — products without a price in the price list are hidden.
          For dealers, their personal price list is checked. For public access — any price list.
          Empty catalog tree nodes are also hidden.
        </p>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <Toggle
            label="Priced only — for admins"
            description="Applies to the admin panel and API with ADMIN/MANAGE_* permissions"
            checked={settings?.requirePriceAdmin ?? false}
            onChange={v => handleToggle('requirePriceAdmin', v)}
            disabled={!settings || saving}
          />
          <Toggle
            label="Priced only — for dealers"
            description="Hides products without a price from the dealer's personal price list"
            checked={settings?.requirePriceDealer ?? false}
            onChange={v => handleToggle('requirePriceDealer', v)}
            disabled={!settings || saving}
          />
          <Toggle
            label="Priced only — for public"
            description="Applies to the public catalog and search"
            checked={settings?.requirePricePublic ?? false}
            onChange={v => handleToggle('requirePricePublic', v)}
            disabled={!settings || saving}
          />
        </div>
      </Section>

      {/* --- Statistics --- */}
      <Section title="System Statistics">
        <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 12 }}>
          <button className="btn" onClick={loadStats} disabled={statsLoading} style={{ fontSize: 13 }}>
            {statsLoading ? 'Refreshing...' : 'Refresh'}
          </button>
        </div>

        {stats && (
          <>
            <h4 style={{ margin: '0 0 10px', fontSize: 13, color: 'var(--wpw-text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
              Products &amp; Media
            </h4>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, marginBottom: 20 }}>
              <StatCard label="Active products" value={stats.totalActiveProducts} />
              <StatCard label="With images" value={stats.productsWithOwnMedia} sub={pct(stats.mediaCoveragePct) + ' coverage'} />
              <StatCard label="Without images" value={stats.productsWithoutOwnMedia} />
              <StatCard label="Total media files" value={stats.totalMediaFiles} />
            </div>

            <h4 style={{ margin: '0 0 10px', fontSize: 13, color: 'var(--wpw-text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
              Public Price List (Stock)
            </h4>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, marginBottom: 20 }}>
              <StatCard label="Price list rows" value={stats.stockPriceListItems} />
              <StatCard label="Products in price list" value={stats.productsInStockPriceList} />
              <StatCard label="Active with price" value={stats.activeProductsWithStockPrice} sub={pct(stats.stockPriceCoveragePct) + ' coverage'} />
              <StatCard label="Active without price" value={stats.activeProductsWithoutStockPrice} />
            </div>

            <h4 style={{ margin: '0 0 10px', fontSize: 13, color: 'var(--wpw-text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
              Dealers
            </h4>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, marginBottom: 20 }}>
              <StatCard label="Total dealers" value={stats.totalDealers} />
              <StatCard label="Active dealers" value={stats.activeDealers} />
              <StatCard label="With price list" value={stats.dealersWithPriceList} />
              <StatCard label="Without price list" value={stats.dealersWithoutPriceList} />
              <StatCard label="With SKU mapping" value={stats.dealersWithSkuMapping} />
              <StatCard label="Total SKU mappings" value={stats.totalSkuMappings} />
            </div>

            <h4 style={{ margin: '0 0 10px', fontSize: 13, color: 'var(--wpw-text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
              Catalog
            </h4>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, marginBottom: 12 }}>
              <StatCard label="Sections" value={stats.totalSections} />
              <StatCard label="Categories" value={stats.totalCategories} />
              <StatCard label="Product groups" value={stats.totalProductGroups} />
              <StatCard label="Empty groups" value={stats.emptyProductGroups} />
              <StatCard label="Nodes with image" value={stats.catalogNodesWithImage} />
            </div>

            <div style={{ fontSize: 11, color: 'var(--wpw-text-muted)', marginTop: 8 }}>
              Data as of: {new Date(stats.generatedAt).toLocaleString('en-US')}
            </div>
          </>
        )}

        {statsLoading && !stats && (
          <div style={{ color: 'var(--wpw-text-muted)', fontSize: 14 }}>Loading statistics...</div>
        )}
      </Section>

      {/* --- Notification Emails --- */}
      <Section title="Order Email Notifications">
        <NotificationEmailsSection />
      </Section>

      {/* --- Danger Zone --- */}
      <Section title="Danger Zone">
        <div style={{
          border: '1px solid #fca5a5',
          borderRadius: 8,
          padding: 16,
          background: '#fff5f5',
        }}>
          <div style={{ fontWeight: 600, marginBottom: 4, color: '#dc2626' }}>
            Delete all product media
          </div>
          <div style={{ fontSize: 13, color: '#7f1d1d', marginBottom: 12 }}>
            Deletes all MediaFile records from the database and physical files on disk.
            Section, category and group images are <strong>not affected</strong>.
            This action is irreversible.
          </div>

          {!deleteConfirm ? (
            <button
              className="btn"
              style={{ background: '#dc2626', color: '#fff', border: 'none' }}
              onClick={() => setDeleteConfirm(true)}
            >
              Delete all product images
            </button>
          ) : (
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
              <span style={{ fontSize: 13, fontWeight: 500, color: '#dc2626' }}>
                Are you sure? This action cannot be undone.
              </span>
              <button
                className="btn"
                style={{ background: '#dc2626', color: '#fff', border: 'none' }}
                onClick={handleDeleteAll}
                disabled={deleting}
              >
                {deleting ? 'Deleting...' : 'Yes, delete all'}
              </button>
              <button className="btn" onClick={() => setDeleteConfirm(false)} disabled={deleting}>
                Cancel
              </button>
            </div>
          )}
        </div>
      </Section>
    </div>
  );
}
