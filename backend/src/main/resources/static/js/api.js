/**
 * WPW PIM — API Module
 * All fetch calls to /api/v1 are centralised here.
 * Exported as window.API for use by other modules.
 */

const API = (() => {
  const BASE = '/api/v1';

  /**
   * Core fetch wrapper with error handling.
   * Throws a structured Error with .status and .body on HTTP failures.
   */
  async function request(path, options = {}) {
    const url = BASE + path;
    let response;
    try {
      response = await fetch(url, options);
    } catch (networkErr) {
      const err = new Error('Network error — check your connection or the server status.');
      err.status = 0;
      throw err;
    }

    if (!response.ok) {
      let body = '';
      try { body = await response.text(); } catch (_) {}
      const err = new Error(body || `HTTP ${response.status} ${response.statusText}`);
      err.status = response.status;
      err.body = body;
      throw err;
    }

    // Handle file downloads (blob) vs JSON
    const contentType = response.headers.get('content-type') || '';
    if (contentType.includes('application/json')) {
      return response.json();
    }
    return response; // Return raw response for blob/text consumers
  }

  function buildQuery(params) {
    const q = new URLSearchParams();
    for (const [key, val] of Object.entries(params)) {
      if (val === null || val === undefined || val === '') continue;
      if (Array.isArray(val)) {
        val.forEach(v => { if (v !== '' && v != null) q.append(key, v); });
      } else {
        q.set(key, val);
      }
    }
    return q.toString();
  }

  // ── Health ────────────────────────────────────────────────
  async function checkHealth() {
    try {
      await fetch(`${BASE}/categories?locale=en`);
      return true;
    } catch {
      return false;
    }
  }

  // ── Categories ────────────────────────────────────────────
  function getCategories(locale = 'en') {
    return request(`/categories?${buildQuery({ locale })}`);
  }

  // ── Products ──────────────────────────────────────────────
  function getProducts(filters = {}) {
    const {
      locale = 'en',
      page = 1,
      perPage = 48,
      operation,
      toolMaterial,
      workpieceMaterial,
      machineType,
      machineBrand,
      cuttingType,
      dMmMin,
      dMmMax,
      shankMm,
      hasBallBearing,
      productType,
      inStock,
    } = filters;

    const params = buildQuery({
      locale, page, perPage,
      operation, toolMaterial, workpieceMaterial,
      machineType, machineBrand, cuttingType,
      dMmMin, dMmMax, shankMm, hasBallBearing,
      productType, inStock,
    });
    return request(`/products?${params}`);
  }

  function getProduct(toolNo, locale = 'en') {
    return request(`/products/${encodeURIComponent(toolNo)}?locale=${locale}`);
  }

  function getSpareParts(productId, locale = 'en') {
    return request(`/products/${productId}/spare-parts?locale=${locale}`);
  }

  function getCompatibleTools(productId, locale = 'en') {
    return request(`/products/${productId}/compatible-tools?locale=${locale}`);
  }

  // ── Search ────────────────────────────────────────────────
  function search(q, locale = 'en', page = 1, perPage = 20) {
    const params = buildQuery({ q, locale, page, perPage });
    return request(`/search?${params}`);
  }

  // ── Operations ────────────────────────────────────────────
  function getOperations() {
    return request('/operations');
  }

  function getOperationProducts(code, locale = 'en', page = 1, perPage = 48) {
    const params = buildQuery({ locale, page, perPage });
    return request(`/operations/${encodeURIComponent(code)}/products?${params}`);
  }

  // ── Export ────────────────────────────────────────────────
  async function exportProducts(format, locale = 'en', extraFilters = {}) {
    const params = buildQuery({ format, locale, ...extraFilters });
    const response = await request(`/export?${params}`);
    // response is the raw Response object
    const blob = await response.blob();
    const ext = format === 'xlsx' ? 'xlsx' : format === 'xml' ? 'xml' : 'csv';
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `wpw-products-${locale}.${ext}`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  // ── Admin Import ──────────────────────────────────────────
  async function validateImport(file) {
    const fd = new FormData();
    fd.append('file', file);
    const response = await fetch(`${BASE}/admin/import/validate`, {
      method: 'POST',
      body: fd,
    });
    if (!response.ok) {
      const body = await response.text();
      const err = new Error(body || `HTTP ${response.status}`);
      err.status = response.status;
      throw err;
    }
    return response.json();
  }

  async function executeImport(file) {
    const fd = new FormData();
    fd.append('file', file);
    const response = await fetch(`${BASE}/admin/import/execute`, {
      method: 'POST',
      body: fd,
    });
    if (!response.ok) {
      const body = await response.text();
      const err = new Error(body || `HTTP ${response.status}`);
      err.status = response.status;
      throw err;
    }
    return response.text(); // Returns markdown report
  }

  // ── Dealer ────────────────────────────────────────────────
  function dealerHeaders(apiKey) {
    return { 'X-Api-Key': apiKey };
  }

  function getPriceList(apiKey) {
    return request('/dealer/price-list', { headers: dealerHeaders(apiKey) });
  }

  function getSkuMapping(apiKey) {
    return request('/dealer/sku-mapping', { headers: dealerHeaders(apiKey) });
  }

  function addSkuMapping(apiKey, toolNo, dealerSku, note = '') {
    return request('/dealer/sku-mapping', {
      method: 'POST',
      headers: {
        ...dealerHeaders(apiKey),
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ toolNo, dealerSku, note }),
    });
  }

  return {
    checkHealth,
    getCategories,
    getProducts,
    getProduct,
    getSpareParts,
    getCompatibleTools,
    search,
    getOperations,
    getOperationProducts,
    exportProducts,
    validateImport,
    executeImport,
    getPriceList,
    getSkuMapping,
    addSkuMapping,
  };
})();

window.API = API;
