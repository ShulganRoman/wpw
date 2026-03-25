/**
 * WPW PIM — Shared UI Components
 * All functions return HTML strings or DOM nodes.
 * Exported as window.UI
 */

const UI = (() => {

  // ── SVG Icons ──────────────────────────────────────────────
  const Icons = {
    search: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path stroke-linecap="round" d="m21 21-4.35-4.35"/></svg>`,
    catalog: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>`,
    operations: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M12 1v4M12 19v4M4.22 4.22l2.83 2.83M16.95 16.95l2.83 2.83M1 12h4M19 12h4M4.22 19.78l2.83-2.83M16.95 7.05l2.83-2.83"/></svg>`,
    export: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 0 0 3 3h10a3 3 0 0 0 3-3v-1m-4-4-4 4m0 0-4-4m4 4V4"/></svg>`,
    admin: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4M7.835 4.697a3.42 3.42 0 0 0 1.946-.806 3.42 3.42 0 0 1 4.438 0 3.42 3.42 0 0 0 1.946.806 3.42 3.42 0 0 1 3.138 3.138 3.42 3.42 0 0 0 .806 1.946 3.42 3.42 0 0 1 0 4.438 3.42 3.42 0 0 0-.806 1.946 3.42 3.42 0 0 1-3.138 3.138 3.42 3.42 0 0 0-1.946.806 3.42 3.42 0 0 1-4.438 0 3.42 3.42 0 0 0-1.946-.806 3.42 3.42 0 0 1-3.138-3.138 3.42 3.42 0 0 0-.806-1.946 3.42 3.42 0 0 1 0-4.438 3.42 3.42 0 0 0 .806-1.946 3.42 3.42 0 0 1 3.138-3.138z"/></svg>`,
    dealer: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M19 21V5a2 2 0 0 0-2-2H7a2 2 0 0 0-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v5m-4 0h4"/></svg>`,
    upload: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 0 0 3 3h10a3 3 0 0 0 3-3v-1m-4-8-4-4m0 0L8 8m4-4v12"/></svg>`,
    chevronRight: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="m9 18 6-6-6-6"/></svg>`,
    check: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"/></svg>`,
    error: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path stroke-linecap="round" d="M12 8v4m0 4h.01"/></svg>`,
    warning: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M12 9v4m0 4h.01M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/></svg>`,
    info: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path stroke-linecap="round" d="M12 16v-4m0-4h.01"/></svg>`,
    tool: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5"><path stroke-linecap="round" stroke-linejoin="round" d="M11.42 15.17 17.25 21A2.652 2.652 0 0 0 21 17.25l-5.877-5.877M11.42 15.17l2.496-3.03c.317-.384.74-.626 1.208-.766M11.42 15.17l-4.655 5.653a2.548 2.548 0 1 1-3.586-3.586l6.837-5.63m5.108-.233c.55-.164 1.163-.188 1.743-.14a4.5 4.5 0 0 0 4.486-6.336l-3.276 3.277a3.004 3.004 0 0 1-2.25-2.25l3.276-3.276a4.5 4.5 0 0 0-6.336 4.486c.091 1.076-.071 2.264-.904 2.95l-.102.085m-1.745 1.437L5.909 7.5H4.5L2.25 3.75l1.5-1.5L7.5 4.5v1.409l4.26 4.26m-1.745 1.437 1.745-1.437m6.615 8.206L15.75 15.75M4.867 19.125h.008v.008h-.008v-.008z"/></svg>`,
    star: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>`,
    download: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 0 0 3 3h10a3 3 0 0 0 3-3v-1m-4-4-4 4m0 0-4-4m4 4V4"/></svg>`,
    ai: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M9.813 15.904 9 18.75l-.813-2.846a4.5 4.5 0 0 0-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 0 0 3.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 0 0 3.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 0 0-3.09 3.09z"/></svg>`,
    close: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12"/></svg>`,
    empty: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5"><path stroke-linecap="round" stroke-linejoin="round" d="M20.25 7.5l-.625 10.632a2.25 2.25 0 0 1-2.247 2.118H6.622a2.25 2.25 0 0 1-2.247-2.118L3.75 7.5M10 11.25h4M3.375 7.5h17.25c.621 0 1.125-.504 1.125-1.125v-1.5c0-.621-.504-1.125-1.125-1.125H3.375c-.621 0-1.125.504-1.125 1.125v1.5c0 .621.504 1.125 1.125 1.125z"/></svg>`,
    key: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M15.75 5.25a3 3 0 0 1 3 3m3 0a6 6 0 0 1-7.029 5.912c-.563-.097-1.159.026-1.563.43L10.5 17.25H8.25v2.25H6v2.25H2.25v-2.818c0-.597.237-1.17.659-1.591l6.499-6.499c.404-.404.527-1 .43-1.563A6 6 0 0 1 21.75 8.25z"/></svg>`,
    refresh: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 0 0 4.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 0 1-15.357-2m15.357 2H15"/></svg>`,
    plus: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4"/></svg>`,
  };

  // ── Tool SVG placeholder ───────────────────────────────────
  const TOOL_PLACEHOLDER_SVG = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 80 80" fill="none" stroke="#C8D8E8" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="40" cy="40" r="28" stroke="#E0ECF5"/><rect x="37" y="12" width="6" height="30" rx="1"/><ellipse cx="40" cy="14" rx="4" ry="3"/><line x1="33" y1="44" x2="47" y2="44"/><path d="M33 44 c-2 4-3 8-3 12 l20 0 c0-4-1-8-3-12"/><line x1="36" y1="44" x2="36" y2="56"/><line x1="40" y1="44" x2="40" y2="56"/><line x1="44" y1="44" x2="44" y2="56"/></svg>`;

  // ── Loading state ──────────────────────────────────────────
  function loadingState(text = 'Loading…') {
    return `<div class="loading-state">
      <div class="loading-spinner"></div>
      <div class="loading-text">${text}</div>
    </div>`;
  }

  // ── Skeleton card grid ─────────────────────────────────────
  function skeletonCards(count = 8) {
    const card = `<div class="skeleton-card">
      <div class="skeleton skeleton-img"></div>
      <div class="skeleton-body">
        <div class="skeleton skeleton-line w-40"></div>
        <div class="skeleton skeleton-line w-90"></div>
        <div class="skeleton skeleton-line w-70"></div>
      </div>
    </div>`;
    return `<div class="products-grid">${Array(count).fill(card).join('')}</div>`;
  }

  // ── Empty state ────────────────────────────────────────────
  function emptyState(title = 'No results', sub = '', extraHtml = '') {
    return `<div class="empty-state">
      ${Icons.empty}
      <div class="empty-state-title">${title}</div>
      ${sub ? `<div class="empty-state-sub">${sub}</div>` : ''}
      ${extraHtml}
    </div>`;
  }

  // ── Error banner ───────────────────────────────────────────
  function errorBanner(msg) {
    return `<div class="error-banner">
      ${Icons.error}
      <div>${escapeHtml(msg)}</div>
    </div>`;
  }

  // ── Success banner ─────────────────────────────────────────
  function successBanner(msg) {
    return `<div class="success-banner">
      ${Icons.check}
      <div>${escapeHtml(msg)}</div>
    </div>`;
  }

  // ── Toast ──────────────────────────────────────────────────
  function toast(msg, type = 'success', duration = 4000) {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const iconMap = { success: Icons.check, error: Icons.error, warning: Icons.warning, info: Icons.info };
    const el = document.createElement('div');
    el.className = `toast toast-${type}`;
    el.innerHTML = `${iconMap[type] || Icons.info}<span class="toast-msg">${escapeHtml(msg)}</span>`;
    container.appendChild(el);

    setTimeout(() => {
      el.style.animation = 'toastOut 0.3s ease-in forwards';
      setTimeout(() => el.remove(), 300);
    }, duration);
  }

  // ── Product Card ───────────────────────────────────────────
  function productCard(p, onClick) {
    const stockMap = {
      in_stock: ['in-stock', 'In Stock'],
      low_stock: ['low-stock', 'Low Stock'],
      out_of_stock: ['out-of-stock', 'Out of Stock'],
    };
    const [stockClass, stockLabel] = stockMap[p.stockStatus] || ['', ''];

    const imageHtml = p.thumbnailUrl
      ? `<img src="${escapeAttr(p.thumbnailUrl)}" alt="${escapeAttr(p.name || p.toolNo)}" loading="lazy" onerror="this.parentNode.innerHTML='<div class=\\'product-card-placeholder\\'>${TOOL_PLACEHOLDER_SVG}</div>'">`
      : `<div class="product-card-placeholder">${TOOL_PLACEHOLDER_SVG}</div>`;

    const attrs = [];
    if (p.dMm) attrs.push(`D: ${p.dMm}mm`);
    if (p.shankMm) attrs.push(`S: ${p.shankMm}mm`);
    if (p.cuttingType) attrs.push(p.cuttingType.replace(/_/g, ' '));
    if (p.flutes) attrs.push(`${p.flutes}F`);

    const id = `card-${escapeAttr(p.toolNo)}`;
    const dir = p.isRtl ? 'dir="rtl"' : '';

    return `<div class="product-card" id="${id}" ${dir} data-toolno="${escapeAttr(p.toolNo)}" onclick="(${onClick.toString()})('${escapeAttr(p.toolNo)}')">
      <div class="product-card-image">
        ${imageHtml}
        ${stockClass ? `<span class="stock-badge ${stockClass}">${stockLabel}</span>` : ''}
      </div>
      <div class="product-card-body">
        <div class="product-card-tool-no">${escapeHtml(p.toolNo)}</div>
        <div class="product-card-name">${escapeHtml(p.name || '—')}</div>
        ${attrs.length ? `<div class="product-card-attrs">${attrs.map(a => `<span class="attr-chip">${escapeHtml(a)}</span>`).join('')}</div>` : ''}
      </div>
    </div>`;
  }

  // ── Pagination ─────────────────────────────────────────────
  function pagination(currentPage, totalPages, onPageChange) {
    if (totalPages <= 1) return '';

    const maxVisible = 7;
    const half = Math.floor(maxVisible / 2);
    let start = Math.max(1, currentPage - half);
    let end = Math.min(totalPages, start + maxVisible - 1);
    if (end - start < maxVisible - 1) start = Math.max(1, end - maxVisible + 1);

    const fnName = `__paginateGo_${Date.now()}`;
    window[fnName] = (p) => { delete window[fnName]; onPageChange(p); };

    let pages = '';
    if (start > 1) {
      pages += `<button class="page-btn" onclick="${fnName}(1)">1</button>`;
      if (start > 2) pages += `<span class="page-info">…</span>`;
    }
    for (let p = start; p <= end; p++) {
      pages += `<button class="page-btn${p === currentPage ? ' current' : ''}" onclick="${fnName}(${p})">${p}</button>`;
    }
    if (end < totalPages) {
      if (end < totalPages - 1) pages += `<span class="page-info">…</span>`;
      pages += `<button class="page-btn" onclick="${fnName}(${totalPages})">${totalPages}</button>`;
    }

    return `<div class="pagination">
      <button class="page-btn" onclick="${fnName}(${currentPage - 1})" ${currentPage <= 1 ? 'disabled' : ''}>&#8592; Prev</button>
      ${pages}
      <button class="page-btn" onclick="${fnName}(${currentPage + 1})" ${currentPage >= totalPages ? 'disabled' : ''}>Next &#8594;</button>
    </div>`;
  }

  // ── Locale switcher ────────────────────────────────────────
  function localeSwitcher(current, onChange) {
    const locales = [
      { code: 'en', label: 'EN' },
      { code: 'he', label: 'HE' },
      { code: 'ru', label: 'RU' },
      { code: 'de', label: 'DE' },
    ];
    const fnName = `__localeChange_${Date.now()}`;
    window[fnName] = (code) => { delete window[fnName]; onChange(code); };
    return `<div class="locale-switcher">
      ${locales.map(l => `<button class="locale-btn${l.code === current ? ' active' : ''}" onclick="${fnName}('${l.code}')">${l.label}</button>`).join('')}
    </div>`;
  }

  // ── Breadcrumbs ────────────────────────────────────────────
  function breadcrumbs(items) {
    // items: [{label, href?}]
    return `<div class="breadcrumbs">
      ${items.map((item, i) => {
        if (i === items.length - 1) {
          return `<span class="breadcrumb-current">${escapeHtml(item.label)}</span>`;
        }
        const link = item.href
          ? `<a onclick="Router.navigate('${item.href}')">${escapeHtml(item.label)}</a>`
          : `<span>${escapeHtml(item.label)}</span>`;
        return link + `<span class="breadcrumbs-sep">›</span>`;
      }).join('')}
    </div>`;
  }

  // ── Badge chip ─────────────────────────────────────────────
  function badge(text, variant = '') {
    return `<span class="badge-item ${variant}">${escapeHtml(text.replace(/_/g, ' '))}</span>`;
  }

  // ── Spec table row ─────────────────────────────────────────
  function specRow(label, value, unit = '') {
    if (value == null || value === false || value === '') return '';
    const display = value === true ? 'Yes' : `${value}${unit}`;
    return `<tr><td>${escapeHtml(label)}</td><td>${escapeHtml(String(display))}</td></tr>`;
  }

  // ── Escape helpers ─────────────────────────────────────────
  function escapeHtml(str) {
    if (str == null) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function escapeAttr(str) {
    if (str == null) return '';
    return String(str).replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  // ── Highlight search terms in text ─────────────────────────
  function highlight(text, query) {
    if (!query || !text) return escapeHtml(text);
    const safe = escapeHtml(text);
    const q = escapeHtml(query).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    return safe.replace(new RegExp(`(${q})`, 'gi'), '<mark class="highlight">$1</mark>');
  }

  // ── Format currency ────────────────────────────────────────
  function formatPrice(price, symbol = '$') {
    const n = parseFloat(price);
    if (isNaN(n)) return '—';
    return `${symbol}${n.toFixed(2)}`;
  }

  return {
    Icons,
    TOOL_PLACEHOLDER_SVG,
    loadingState,
    skeletonCards,
    emptyState,
    errorBanner,
    successBanner,
    toast,
    productCard,
    pagination,
    localeSwitcher,
    breadcrumbs,
    badge,
    specRow,
    escapeHtml,
    escapeAttr,
    highlight,
    formatPrice,
  };
})();

window.UI = UI;
