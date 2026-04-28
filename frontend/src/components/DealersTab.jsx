import { useState, useEffect } from 'react';
import { getDealers, createDealer, updateDealer, deleteDealer, resetDealerPassword } from '../api/api';
import { useToast } from './ToastContext';

const DEALER_TYPES = ['Distributor', 'Reseller', 'Online Store', 'OEM Partner'];
const CURRENCIES = ['EUR', 'USD', 'ILS', 'GBP', 'AUD', 'PLN', 'CAD', 'CHF'];
const CONTACT_ROLES = ['Sales', 'Technical', 'Management', 'Purchasing'];

function SectionToggle({ title, required, children, defaultOpen = false }) {
  const [open, setOpen] = useState(defaultOpen);
  return (
    <div style={{ marginBottom: 4 }}>
      <button
        type="button"
        onClick={() => setOpen(v => !v)}
        style={{
          width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          padding: '8px 12px', background: open ? 'var(--wpw-light-blue)' : '#f5f7fa',
          border: '1px solid var(--wpw-border)', borderRadius: 'var(--wpw-radius)',
          cursor: 'pointer', fontSize: 13, fontWeight: 600, color: 'var(--wpw-navy)',
          transition: 'background 0.15s',
        }}
      >
        <span>
          {title}
          {required && <span style={{ color: '#c62828', marginLeft: 4 }}>*</span>}
        </span>
        <span style={{ fontSize: 10, color: 'var(--wpw-mid-gray)', transform: open ? 'rotate(180deg)' : 'none', transition: 'transform 0.15s' }}>▼</span>
      </button>
      {open && (
        <div style={{ padding: '12px 12px 4px', border: '1px solid var(--wpw-border)', borderTop: 'none', borderRadius: '0 0 var(--wpw-radius) var(--wpw-radius)', background: '#fff' }}>
          {children}
        </div>
      )}
    </div>
  );
}

function FieldRow({ label, required, children }) {
  return (
    <div style={{ display: 'grid', gridTemplateColumns: '140px 1fr', gap: 8, marginBottom: 10, alignItems: 'center' }}>
      <label style={{ fontSize: 12, fontWeight: 600, color: 'var(--wpw-gray)', textAlign: 'right', paddingRight: 4 }}>
        {label}{required && <span style={{ color: '#c62828' }}> *</span>}
      </label>
      {children}
    </div>
  );
}

function FInput({ value, onChange, placeholder, type = 'text', style }) {
  return (
    <input
      className="input"
      type={type}
      value={value ?? ''}
      onChange={e => onChange(e.target.value)}
      placeholder={placeholder}
      style={{ width: '100%', ...style }}
    />
  );
}

function FSelect({ value, onChange, options, placeholder }) {
  return (
    <select
      className="input"
      value={value ?? ''}
      onChange={e => onChange(e.target.value)}
      style={{ width: '100%' }}
    >
      <option value="">{placeholder || '— выберите —'}</option>
      {options.map(o => (
        <option key={o} value={o}>{o}</option>
      ))}
    </select>
  );
}

function FCheck({ label, checked, onChange }) {
  return (
    <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 13, cursor: 'pointer' }}>
      <input type="checkbox" checked={!!checked} onChange={e => onChange(e.target.checked)} />
      {label}
    </label>
  );
}

function ContactRow({ contact, index, onChange, onRemove }) {
  const upd = (field, val) => onChange(index, { ...contact, [field]: val });
  return (
    <div style={{ background: '#f9fbfd', border: '1px solid var(--wpw-border)', borderRadius: 'var(--wpw-radius)', padding: '10px 12px', marginBottom: 8 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
        <span style={{ fontSize: 12, fontWeight: 600, color: 'var(--wpw-gray)' }}>Контакт {index + 1}</span>
        <button type="button" className="btn btn-secondary" style={{ padding: '2px 8px', fontSize: 11, color: '#c62828' }} onClick={() => onRemove(index)}>✕ Удалить</button>
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
        <div>
          <div style={{ fontSize: 11, color: 'var(--wpw-mid-gray)', marginBottom: 3 }}>Имя <span style={{ color: '#c62828' }}>*</span></div>
          <FInput value={contact.contactName} onChange={v => upd('contactName', v)} placeholder="Имя контакта" />
        </div>
        <div>
          <div style={{ fontSize: 11, color: 'var(--wpw-mid-gray)', marginBottom: 3 }}>Роль</div>
          <FSelect value={contact.role} onChange={v => upd('role', v)} options={CONTACT_ROLES} />
        </div>
        <div>
          <div style={{ fontSize: 11, color: 'var(--wpw-mid-gray)', marginBottom: 3 }}>Email</div>
          <FInput value={contact.email} onChange={v => upd('email', v)} placeholder="email@company.com" type="email" />
        </div>
        <div>
          <div style={{ fontSize: 11, color: 'var(--wpw-mid-gray)', marginBottom: 3 }}>Телефон</div>
          <FInput value={contact.phone} onChange={v => upd('phone', v)} placeholder="+1 555 000 0000" />
        </div>
      </div>
      <div style={{ marginTop: 8 }}>
        <FCheck label="Основной контакт" checked={contact.isPrimary} onChange={v => upd('isPrimary', v)} />
      </div>
    </div>
  );
}

function emptyDealer() {
  return {
    dealerCode: '', companyName: '', country: '',
    brandName: '', dealerType: '', privateLabelBrand: '',
    region: '', city: '', address: '', postalCode: '',
    website: '', hasEcommerce: false, shopUrl: '', logo: '',
    priceListId: '', currency: '', discountTier: '',
    notes: '', isActive: true,
    contacts: [],
  };
}

function DealerModal({ dealer, onSave, onClose }) {
  const [form, setForm] = useState(() => dealer ? {
    dealerCode: dealer.dealerCode ?? '',
    companyName: dealer.companyName ?? '',
    country: dealer.country ?? '',
    brandName: dealer.brandName ?? '',
    dealerType: dealer.dealerType ?? '',
    privateLabelBrand: dealer.privateLabelBrand ?? '',
    region: dealer.region ?? '',
    city: dealer.city ?? '',
    address: dealer.address ?? '',
    postalCode: dealer.postalCode ?? '',
    website: dealer.website ?? '',
    hasEcommerce: dealer.hasEcommerce ?? false,
    shopUrl: dealer.shopUrl ?? '',
    logo: dealer.logo ?? '',
    priceListId: dealer.priceListId ?? '',
    currency: dealer.currency ?? '',
    discountTier: dealer.discountTier ?? '',
    notes: dealer.notes ?? '',
    isActive: dealer.isActive ?? true,
    contacts: dealer.contacts ?? [],
  } : emptyDealer());

  const [saving, setSaving] = useState(false);
  const set = (field, val) => setForm(f => ({ ...f, [field]: val }));

  function addContact() {
    setForm(f => ({ ...f, contacts: [...f.contacts, { contactName: '', role: '', email: '', phone: '', isPrimary: false }] }));
  }

  function updateContact(idx, contact) {
    setForm(f => {
      const contacts = [...f.contacts];
      contacts[idx] = contact;
      return { ...f, contacts };
    });
  }

  function removeContact(idx) {
    setForm(f => ({ ...f, contacts: f.contacts.filter((_, i) => i !== idx) }));
  }

  async function handleSave() {
    if (!form.dealerCode.trim()) { alert('Введите код дилера'); return; }
    if (!form.companyName.trim()) { alert('Введите название компании'); return; }
    if (!form.country.trim()) { alert('Введите страну'); return; }
    setSaving(true);
    try {
      await onSave(form);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div style={{
      position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.45)', zIndex: 1000,
      display: 'flex', alignItems: 'flex-start', justifyContent: 'center', padding: '32px 16px', overflowY: 'auto',
    }} onClick={e => e.target === e.currentTarget && onClose()}>
      <div style={{
        background: '#fff', borderRadius: 10, width: '100%', maxWidth: 640,
        boxShadow: '0 8px 32px rgba(0,0,0,0.18)', padding: 0, overflow: 'hidden',
      }}>
        {/* Header */}
        <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--wpw-border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: 'var(--wpw-navy)' }}>
          <span style={{ fontSize: 15, fontWeight: 700, color: '#fff' }}>
            {dealer ? 'Редактировать дилера' : 'Добавить дилера'}
          </span>
          <button type="button" onClick={onClose} style={{ background: 'none', border: 'none', color: '#fff', fontSize: 18, cursor: 'pointer', lineHeight: 1, padding: '2px 6px' }}>✕</button>
        </div>

        {/* Body */}
        <div style={{ padding: '16px 20px', maxHeight: 'calc(90vh - 140px)', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 6 }}>

          {/* 1. Основные данные — always open, required */}
          <SectionToggle title="Основные данные" required defaultOpen>
            <FieldRow label="Код дилера" required>
              <FInput value={form.dealerCode} onChange={v => set('dealerCode', v)} placeholder="WPW-001" />
            </FieldRow>
            <FieldRow label="Юр. название" required>
              <FInput value={form.companyName} onChange={v => set('companyName', v)} placeholder="ООО «Компания»" />
            </FieldRow>
            <FieldRow label="Страна" required>
              <FInput value={form.country} onChange={v => set('country', v)} placeholder="Germany" />
            </FieldRow>
          </SectionToggle>

          {/* 2. Торговая информация */}
          <SectionToggle title="Торговая информация">
            <FieldRow label="Торговое назв.">
              <FInput value={form.brandName} onChange={v => set('brandName', v)} placeholder="Торговое название (если отличается)" />
            </FieldRow>
            <FieldRow label="Тип дилера">
              <FSelect value={form.dealerType} onChange={v => set('dealerType', v)} options={DEALER_TYPES} />
            </FieldRow>
            <FieldRow label="Private label">
              <FInput value={form.privateLabelBrand} onChange={v => set('privateLabelBrand', v)} placeholder="Amana, Trend, Freud…" />
            </FieldRow>
          </SectionToggle>

          {/* 3. Адрес */}
          <SectionToggle title="Адрес">
            <FieldRow label="Регион / штат">
              <FInput value={form.region} onChange={v => set('region', v)} placeholder="Bavaria" />
            </FieldRow>
            <FieldRow label="Город">
              <FInput value={form.city} onChange={v => set('city', v)} placeholder="Munich" />
            </FieldRow>
            <FieldRow label="Адрес">
              <FInput value={form.address} onChange={v => set('address', v)} placeholder="Hauptstraße 1" />
            </FieldRow>
            <FieldRow label="Индекс">
              <FInput value={form.postalCode} onChange={v => set('postalCode', v)} placeholder="80331" />
            </FieldRow>
          </SectionToggle>

          {/* 4. Онлайн-присутствие */}
          <SectionToggle title="Онлайн-присутствие">
            <FieldRow label="Сайт">
              <FInput value={form.website} onChange={v => set('website', v)} placeholder="https://example.com" />
            </FieldRow>
            <FieldRow label="Интернет-магазин">
              <FCheck label="Есть интернет-магазин" checked={form.hasEcommerce} onChange={v => set('hasEcommerce', v)} />
            </FieldRow>
            {form.hasEcommerce && (
              <FieldRow label="URL магазина">
                <FInput value={form.shopUrl} onChange={v => set('shopUrl', v)} placeholder="https://shop.example.com" />
              </FieldRow>
            )}
            <FieldRow label="Логотип (URL)">
              <FInput value={form.logo} onChange={v => set('logo', v)} placeholder="https://cdn.example.com/logo.png" />
            </FieldRow>
          </SectionToggle>

          {/* 5. Коммерческая информация */}
          <SectionToggle title="Коммерческая информация">
            <FieldRow label="Валюта">
              <FSelect value={form.currency} onChange={v => set('currency', v)} options={CURRENCIES} />
            </FieldRow>
            <FieldRow label="Уровень скидки">
              <FInput value={form.discountTier} onChange={v => set('discountTier', v)} placeholder="Gold / Silver…" />
            </FieldRow>
            <FieldRow label="ID прайс-листа">
              <FInput value={form.priceListId} onChange={v => set('priceListId', v)} placeholder="UUID прайс-листа (Stage 2)" />
            </FieldRow>
          </SectionToggle>

          {/* 6. Прочее */}
          <SectionToggle title="Прочее">
            <FieldRow label="Заметки">
              <textarea
                className="input"
                value={form.notes ?? ''}
                onChange={e => set('notes', e.target.value)}
                placeholder="Внутренние заметки (не публичные)"
                rows={3}
                style={{ width: '100%', resize: 'vertical' }}
              />
            </FieldRow>
            <FieldRow label="Статус">
              <FCheck label="Дилер активен" checked={form.isActive} onChange={v => set('isActive', v)} />
            </FieldRow>
          </SectionToggle>

          {/* 7. Контакты */}
          <SectionToggle title="Контакты">
            {form.contacts.map((c, i) => (
              <ContactRow key={i} contact={c} index={i} onChange={updateContact} onRemove={removeContact} />
            ))}
            <button type="button" className="btn btn-secondary" style={{ fontSize: 12, padding: '5px 12px', marginTop: 4 }} onClick={addContact}>
              + Добавить контакт
            </button>
          </SectionToggle>

        </div>

        {/* Footer */}
        <div style={{ padding: '12px 20px', borderTop: '1px solid var(--wpw-border)', display: 'flex', justifyContent: 'flex-end', gap: 8, background: '#f9fbfd' }}>
          <button type="button" className="btn btn-secondary" onClick={onClose} disabled={saving}>Отмена</button>
          <button type="button" className="btn btn-primary" onClick={handleSave} disabled={saving}>
            {saving ? 'Сохранение…' : (dealer ? 'Сохранить' : 'Создать')}
          </button>
        </div>
      </div>
    </div>
  );
}

function CredentialsModal({ username, password, onClose }) {
  const [copied, setCopied] = useState(null);

  function copy(text, field) {
    navigator.clipboard.writeText(text).then(() => {
      setCopied(field);
      setTimeout(() => setCopied(null), 2000);
    });
  }

  return (
    <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.55)', zIndex: 1100, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16 }}>
      <div style={{ background: '#fff', borderRadius: 10, width: '100%', maxWidth: 440, boxShadow: '0 8px 32px rgba(0,0,0,0.22)', overflow: 'hidden' }}>
        <div style={{ padding: '16px 20px', background: '#1b5e20', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{ color: '#fff', fontWeight: 700, fontSize: 15 }}>Учётные данные дилера</span>
          <button type="button" onClick={onClose} style={{ background: 'none', border: 'none', color: '#fff', fontSize: 18, cursor: 'pointer', padding: '2px 6px' }}>✕</button>
        </div>
        <div style={{ padding: '20px 24px' }}>
          <div style={{ background: '#fff9c4', border: '1px solid #f9a825', borderRadius: 6, padding: '10px 14px', marginBottom: 20, fontSize: 12, color: '#6d4c00' }}>
            Сохраните пароль — он отображается только один раз и не может быть восстановлен.
          </div>
          {[['Логин', username, 'login'], ['Пароль', password, 'pass']].map(([label, value, key]) => (
            <div key={key} style={{ marginBottom: 14 }}>
              <div style={{ fontSize: 11, fontWeight: 600, color: 'var(--wpw-gray)', marginBottom: 5, textTransform: 'uppercase', letterSpacing: '0.04em' }}>{label}</div>
              <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                <code style={{ flex: 1, background: '#f5f7fa', border: '1px solid var(--wpw-border)', borderRadius: 6, padding: '8px 12px', fontSize: 14, fontFamily: 'monospace', letterSpacing: key === 'pass' ? '0.08em' : 'normal' }}>
                  {value}
                </code>
                <button
                  type="button"
                  className="btn btn-secondary"
                  style={{ padding: '7px 12px', fontSize: 12, minWidth: 70 }}
                  onClick={() => copy(value, key)}
                >
                  {copied === key ? '✓ Готово' : 'Копировать'}
                </button>
              </div>
            </div>
          ))}
          <button
            type="button"
            className="btn btn-primary"
            style={{ width: '100%', marginTop: 8 }}
            onClick={() => copy(`Логин: ${username}\nПароль: ${password}`, 'all')}
          >
            {copied === 'all' ? '✓ Скопировано' : 'Скопировать всё'}
          </button>
        </div>
        <div style={{ padding: '12px 24px', borderTop: '1px solid var(--wpw-border)', display: 'flex', justifyContent: 'flex-end' }}>
          <button type="button" className="btn btn-primary" onClick={onClose}>Закрыть</button>
        </div>
      </div>
    </div>
  );
}

export default function DealersTab({ onSkuMapping }) {
  const [dealers, setDealers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modal, setModal] = useState(null);
  const [confirmDeleteId, setConfirmDeleteId] = useState(null);
  const [credentials, setCredentials] = useState(null); // { username, password }
  const toast = useToast();

  useEffect(() => { load(); }, []);

  async function load() {
    setLoading(true);
    try {
      setDealers(await getDealers());
    } catch (err) {
      toast(err.message, 'error');
    } finally {
      setLoading(false);
    }
  }

  async function handleSave(form) {
    try {
      if (modal.dealer) {
        const updated = await updateDealer(modal.dealer.id, form);
        setDealers(prev => prev.map(d => d.id === updated.id ? updated : d));
        toast('Дилер обновлён', 'success');
        setModal(null);
      } else {
        // create returns { dealer, username, generatedPassword }
        const result = await createDealer(form);
        setDealers(prev => [...prev, result.dealer]);
        setModal(null);
        setCredentials({ username: result.username, password: result.generatedPassword });
        toast('Дилер создан', 'success');
      }
    } catch (err) {
      toast(err.message, 'error');
      throw err;
    }
  }

  async function handleDelete(id) {
    try {
      await deleteDealer(id);
      setDealers(prev => prev.filter(d => d.id !== id));
      setConfirmDeleteId(null);
      toast('Дилер и учётная запись удалены', 'success');
    } catch (err) {
      toast(err.message, 'error');
    }
  }

  async function handleResetPassword(id) {
    try {
      const result = await resetDealerPassword(id);
      setCredentials({ username: result.username, password: result.newPassword });
      toast('Пароль сброшен', 'success');
    } catch (err) {
      toast(err.message, 'error');
    }
  }

  const thStyle = { padding: '8px 12px', textAlign: 'left', fontSize: 11, fontWeight: 700, color: 'var(--wpw-gray)', textTransform: 'uppercase', letterSpacing: '0.04em' };
  const tdStyle = { padding: '10px 12px' };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div>
          <div style={{ fontSize: 15, fontWeight: 700, color: 'var(--wpw-navy)' }}>Дилеры</div>
          <div style={{ fontSize: 12, color: 'var(--wpw-mid-gray)', marginTop: 2 }}>Управление дилерской сетью</div>
        </div>
        <button className="btn btn-primary" onClick={() => setModal({})}>+ Добавить дилера</button>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: 40, color: 'var(--wpw-mid-gray)' }}>
          <div className="spinner" style={{ margin: '0 auto 12px' }} />
          Загрузка…
        </div>
      ) : (
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
            <thead>
              <tr style={{ borderBottom: '2px solid #e8edf5' }}>
                {['Код / Логин', 'Компания', 'Страна', 'Тип', 'Валюта', 'Контактов', 'Статус', ''].map(h => (
                  <th key={h} style={thStyle}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {dealers.map(d => (
                <tr key={d.id} style={{ borderBottom: '1px solid #f0f2f5' }}>
                  <td style={{ ...tdStyle }}>
                    <div style={{ fontFamily: 'var(--wpw-font-mono)', fontSize: 12, color: 'var(--wpw-mid-gray)' }}>{d.dealerCode || '—'}</div>
                    {d.username && (
                      <div style={{ fontSize: 11, color: 'var(--wpw-blue)', marginTop: 2 }}>@{d.username}</div>
                    )}
                  </td>
                  <td style={{ ...tdStyle, fontWeight: 600, color: 'var(--wpw-navy)' }}>
                    {d.companyName || d.name || '—'}
                    {d.brandName && d.brandName !== d.companyName && (
                      <div style={{ fontSize: 11, fontWeight: 400, color: 'var(--wpw-mid-gray)' }}>{d.brandName}</div>
                    )}
                  </td>
                  <td style={tdStyle}>{d.country || '—'}</td>
                  <td style={tdStyle}>{d.dealerType || '—'}</td>
                  <td style={tdStyle}>{d.currency || '—'}</td>
                  <td style={{ ...tdStyle, textAlign: 'center' }}>{d.contacts?.length ?? 0}</td>
                  <td style={tdStyle}>
                    <span style={{ fontSize: 11, padding: '2px 8px', borderRadius: 10, background: d.isActive ? '#e8f5e9' : '#f5f5f5', color: d.isActive ? '#2e7d32' : '#757575', fontWeight: 600 }}>
                      {d.isActive ? 'Активен' : 'Неактивен'}
                    </span>
                  </td>
                  <td style={{ ...tdStyle, whiteSpace: 'nowrap' }}>
                    {confirmDeleteId === d.id ? (
                      <span style={{ display: 'flex', gap: 6, alignItems: 'center', fontSize: 12 }}>
                        <span style={{ color: '#c62828' }}>Удалить дилера и аккаунт?</span>
                        <button className="btn btn-primary" style={{ padding: '3px 10px', fontSize: 12, background: '#c62828', borderColor: '#c62828' }} onClick={() => handleDelete(d.id)}>Да</button>
                        <button className="btn btn-secondary" style={{ padding: '3px 10px', fontSize: 12 }} onClick={() => setConfirmDeleteId(null)}>Нет</button>
                      </span>
                    ) : (
                      <span style={{ display: 'flex', gap: 6 }}>
                        <button className="btn btn-secondary" style={{ padding: '4px 10px', fontSize: 12 }} onClick={() => setModal({ dealer: d })}>Изменить</button>
                        <button className="btn btn-secondary" style={{ padding: '4px 10px', fontSize: 12, color: 'var(--wpw-blue)' }} onClick={() => onSkuMapping?.(d)}>SKU</button>
                        {d.username && (
                          <button className="btn btn-secondary" style={{ padding: '4px 10px', fontSize: 12, color: 'var(--wpw-blue)' }} onClick={() => handleResetPassword(d.id)} title="Сбросить пароль">↺ Пароль</button>
                        )}
                        <button className="btn btn-secondary" style={{ padding: '4px 10px', fontSize: 12, color: '#c62828' }} onClick={() => setConfirmDeleteId(d.id)}>Удалить</button>
                      </span>
                    )}
                  </td>
                </tr>
              ))}
              {dealers.length === 0 && (
                <tr>
                  <td colSpan={8} style={{ padding: '40px 12px', textAlign: 'center', color: 'var(--wpw-mid-gray)', fontSize: 13 }}>
                    Дилеры не добавлены
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {modal && (
        <DealerModal
          dealer={modal.dealer}
          onSave={handleSave}
          onClose={() => setModal(null)}
        />
      )}

      {credentials && (
        <CredentialsModal
          username={credentials.username}
          password={credentials.password}
          onClose={() => setCredentials(null)}
        />
      )}
    </div>
  );
}
