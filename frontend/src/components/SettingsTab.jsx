import { useState, useEffect, useCallback } from 'react';
import { getSystemSettings, updateSystemSettings, getSystemStats, deleteAllProductMedia } from '../api/api';
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

export default function SettingsTab() {
  const { showToast } = useToast();

  const [settings, setSettings] = useState(null);
  const [saving, setSaving] = useState(false);

  const [stats, setStats] = useState(null);
  const [statsLoading, setStatsLoading] = useState(false);

  const [deleteConfirm, setDeleteConfirm] = useState(false);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    getSystemSettings().then(setSettings).catch(() => showToast('Не удалось загрузить настройки', 'error'));
    loadStats();
  }, []);

  const loadStats = useCallback(() => {
    setStatsLoading(true);
    getSystemStats()
      .then(setStats)
      .catch(() => showToast('Не удалось загрузить статистику', 'error'))
      .finally(() => setStatsLoading(false));
  }, []);

  const handleToggle = async (field, value) => {
    const next = { ...settings, [field]: value };
    setSettings(next);
    setSaving(true);
    try {
      await updateSystemSettings(next);
      showToast('Настройки сохранены', 'success');
    } catch {
      showToast('Ошибка сохранения', 'error');
      setSettings(settings); // rollback
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteAll = async () => {
    setDeleting(true);
    try {
      const result = await deleteAllProductMedia();
      showToast(`Удалено: ${result.deletedRecords} записей, ${result.deletedDirectories} папок`, 'success');
      setDeleteConfirm(false);
      loadStats();
    } catch {
      showToast('Ошибка удаления', 'error');
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div style={{ maxWidth: 900 }}>
      {/* --- Visibility --- */}
      <Section title="Видимость товаров">
        <p style={{ fontSize: 13, color: 'var(--wpw-text-muted)', margin: '0 0 16px' }}>
          Когда тумблер включён — в соответствующем разделе отображаются только товары,
          у которых есть собственные изображения (не унаследованные от группы/категории).
        </p>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <Toggle
            label="Только с фото — для администраторов"
            description="Применяется к панели администратора и API с правами ADMIN/MANAGE_*"
            checked={settings?.requireImagesAdmin ?? false}
            onChange={v => handleToggle('requireImagesAdmin', v)}
            disabled={!settings || saving}
          />
          <Toggle
            label="Только с фото — для дилеров"
            description="Применяется к личному кабинету дилера (роль DEALER)"
            checked={settings?.requireImagesDealer ?? false}
            onChange={v => handleToggle('requireImagesDealer', v)}
            disabled={!settings || saving}
          />
          <Toggle
            label="Только с фото — для пользователей"
            description="Применяется к публичному каталогу и поиску"
            checked={settings?.requireImagesPublic ?? false}
            onChange={v => handleToggle('requireImagesPublic', v)}
            disabled={!settings || saving}
          />
        </div>
      </Section>

      {/* --- Price visibility --- */}
      <Section title="Видимость по цене">
        <p style={{ fontSize: 13, color: 'var(--wpw-text-muted)', margin: '0 0 16px' }}>
          Когда тумблер включён — скрываются товары, у которых нет цены в прайс-листе.
          Для дилеров проверяется их персональный прайс-лист. Для публичного доступа — любой прайс-лист.
          Пустые узлы дерева каталога также скрываются.
        </p>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <Toggle
            label="Только с ценой — для администраторов"
            description="Применяется к панели администратора и API с правами ADMIN/MANAGE_*"
            checked={settings?.requirePriceAdmin ?? false}
            onChange={v => handleToggle('requirePriceAdmin', v)}
            disabled={!settings || saving}
          />
          <Toggle
            label="Только с ценой — для дилеров"
            description="Скрывает товары без цены из персонального прайс-листа дилера"
            checked={settings?.requirePriceDealer ?? false}
            onChange={v => handleToggle('requirePriceDealer', v)}
            disabled={!settings || saving}
          />
          <Toggle
            label="Только с ценой — для пользователей"
            description="Применяется к публичному каталогу и поиску"
            checked={settings?.requirePricePublic ?? false}
            onChange={v => handleToggle('requirePricePublic', v)}
            disabled={!settings || saving}
          />
        </div>
      </Section>

      {/* --- Statistics --- */}
      <Section title="Статистика системы">
        <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 12 }}>
          <button className="btn" onClick={loadStats} disabled={statsLoading} style={{ fontSize: 13 }}>
            {statsLoading ? 'Обновление...' : 'Обновить'}
          </button>
        </div>

        {stats && (
          <>
            <h4 style={{ margin: '0 0 10px', fontSize: 13, color: 'var(--wpw-text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
              Товары и медиафайлы
            </h4>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, marginBottom: 20 }}>
              <StatCard label="Активных товаров" value={stats.totalActiveProducts} />
              <StatCard label="С изображениями" value={stats.productsWithOwnMedia} sub={pct(stats.mediaCoveragePct) + ' покрытие'} />
              <StatCard label="Без изображений" value={stats.productsWithoutOwnMedia} />
              <StatCard label="Медиафайлов всего" value={stats.totalMediaFiles} />
            </div>

            <h4 style={{ margin: '0 0 10px', fontSize: 13, color: 'var(--wpw-text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
              Публичный прайс-лист (Stock)
            </h4>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, marginBottom: 20 }}>
              <StatCard label="Строк в прайс-листе" value={stats.stockPriceListItems} />
              <StatCard label="Товаров в прайс-листе" value={stats.productsInStockPriceList} />
              <StatCard label="Активных с ценой" value={stats.activeProductsWithStockPrice} sub={pct(stats.stockPriceCoveragePct) + ' покрытие'} />
              <StatCard label="Активных без цены" value={stats.activeProductsWithoutStockPrice} />
            </div>

            <h4 style={{ margin: '0 0 10px', fontSize: 13, color: 'var(--wpw-text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
              Дилеры
            </h4>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, marginBottom: 20 }}>
              <StatCard label="Всего дилеров" value={stats.totalDealers} />
              <StatCard label="Активных дилеров" value={stats.activeDealers} />
              <StatCard label="С прайс-листом" value={stats.dealersWithPriceList} />
              <StatCard label="Без прайс-листа" value={stats.dealersWithoutPriceList} />
              <StatCard label="С SKU-маппингом" value={stats.dealersWithSkuMapping} />
              <StatCard label="Всего SKU-маппингов" value={stats.totalSkuMappings} />
            </div>

            <h4 style={{ margin: '0 0 10px', fontSize: 13, color: 'var(--wpw-text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
              Каталог
            </h4>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, marginBottom: 12 }}>
              <StatCard label="Разделов" value={stats.totalSections} />
              <StatCard label="Категорий" value={stats.totalCategories} />
              <StatCard label="Групп товаров" value={stats.totalProductGroups} />
              <StatCard label="Пустых групп" value={stats.emptyProductGroups} />
              <StatCard label="Узлов с изображением" value={stats.catalogNodesWithImage} />
            </div>

            <div style={{ fontSize: 11, color: 'var(--wpw-text-muted)', marginTop: 8 }}>
              Данные актуальны на: {new Date(stats.generatedAt).toLocaleString('ru-RU')}
            </div>
          </>
        )}

        {statsLoading && !stats && (
          <div style={{ color: 'var(--wpw-text-muted)', fontSize: 14 }}>Загрузка статистики...</div>
        )}
      </Section>

      {/* --- Danger Zone --- */}
      <Section title="Опасная зона">
        <div style={{
          border: '1px solid #fca5a5',
          borderRadius: 8,
          padding: 16,
          background: '#fff5f5',
        }}>
          <div style={{ fontWeight: 600, marginBottom: 4, color: '#dc2626' }}>
            Удалить все медиафайлы товаров
          </div>
          <div style={{ fontSize: 13, color: '#7f1d1d', marginBottom: 12 }}>
            Удаляет все записи MediaFile из базы данных и физические файлы на диске.
            Изображения разделов, категорий и групп <strong>не затрагиваются</strong>.
            Действие необратимо.
          </div>

          {!deleteConfirm ? (
            <button
              className="btn"
              style={{ background: '#dc2626', color: '#fff', border: 'none' }}
              onClick={() => setDeleteConfirm(true)}
            >
              Удалить все изображения товаров
            </button>
          ) : (
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
              <span style={{ fontSize: 13, fontWeight: 500, color: '#dc2626' }}>
                Вы уверены? Это действие нельзя отменить.
              </span>
              <button
                className="btn"
                style={{ background: '#dc2626', color: '#fff', border: 'none' }}
                onClick={handleDeleteAll}
                disabled={deleting}
              >
                {deleting ? 'Удаление...' : 'Да, удалить всё'}
              </button>
              <button className="btn" onClick={() => setDeleteConfirm(false)} disabled={deleting}>
                Отмена
              </button>
            </div>
          )}
        </div>
      </Section>
    </div>
  );
}
