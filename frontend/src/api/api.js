const BASE = '/api/v1';

function getAuthHeaders() {
  const token = localStorage.getItem('authToken');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function request(path, options = {}) {
  const res = await fetch(`${BASE}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...getAuthHeaders(),
      ...options.headers,
    },
    ...options,
  });
  if (res.status === 401) {
    localStorage.removeItem('authToken');
    localStorage.removeItem('userRole');
    localStorage.removeItem('userPrivileges');
    window.location.hash = '#/login';
    throw new Error('Session expired');
  }
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `HTTP ${res.status}`);
  }
  const ct = res.headers.get('content-type') || '';
  if (ct.includes('application/json')) return res.json();
  return res;
}

export async function login(username, password) {
  const res = await fetch(`${BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });
  if (!res.ok) {
    let message = `HTTP ${res.status}`;
    try {
      const text = await res.text();
      const body = JSON.parse(text);
      message = body.message || body.error || message;
    } catch {
      // text already consumed or not valid JSON — keep default message
    }
    throw new Error(message);
  }
  return res.json();
}

export async function logout() {
  try {
    await fetch(`${BASE}/auth/logout`, {
      method: 'POST',
      headers: { ...getAuthHeaders() },
    });
  } catch {
    // stateless JWT — local cleanup is enough
  }
}

function buildQuery(params) {
  const q = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== null && v !== '') q.set(k, v);
  }
  return q.toString();
}

export async function checkHealth() {
  try {
    await fetch(`${BASE}/categories?locale=en`);
    return true;
  } catch {
    return false;
  }
}

export function getCategories(locale = 'en') {
  return request(`/categories?${buildQuery({ locale })}`);
}

export function getProducts(filters = {}) {
  const params = {
    locale: filters.locale || 'en',
    page: filters.page || 1,
    perPage: filters.perPage || 50,
    sectionId: filters.sectionId || '',
    categoryId: filters.categoryId || '',
    groupId: filters.groupId || '',
    operation: filters.operation || '',
    toolMaterial: filters.toolMaterial || '',
    workpieceMaterial: filters.workpieceMaterial || '',
    machineType: filters.machineType || '',
    machineBrand: filters.machineBrand || '',
    cuttingType: filters.cuttingType || '',
    dMmMin: filters.dMmMin || '',
    dMmMax: filters.dMmMax || '',
    shankMm: filters.shankMm || '',
    hasBallBearing: filters.hasBallBearing || '',
    productType: filters.productType || '',
    inStock: filters.inStock || '',
    priceMin: filters.priceMin || '',
    priceMax: filters.priceMax || '',
  };
  return request(`/products?${buildQuery(params)}`);
}

export function getFilterOptions() {
  return request('/products/filter-options');
}

export function getProduct(toolNo, locale = 'en') {
  return request(`/products/${encodeURIComponent(toolNo)}?locale=${locale}`);
}

export function getSpareParts(productId, locale = 'en') {
  return request(`/products/${productId}/spare-parts?locale=${locale}`);
}

export function getCompatibleTools(productId, locale = 'en') {
  return request(`/products/${productId}/compatible-tools?locale=${locale}`);
}

export function search(q, locale = 'en', page = 1, perPage = 20) {
  return request(`/search?${buildQuery({ q, locale, page, perPage })}`);
}

export function getOperations() {
  return request('/operations');
}

export function createApplicationTag(data) {
  return request('/operations', { method: 'POST', body: JSON.stringify(data) });
}

export function updateApplicationTag(code, data) {
  return request(`/operations/${encodeURIComponent(code)}`, { method: 'PUT', body: JSON.stringify(data) });
}

export function deleteApplicationTag(code) {
  return request(`/operations/${encodeURIComponent(code)}`, { method: 'DELETE' });
}

export function getOperationProducts(code, locale = 'en', page = 1, perPage = 50) {
  return request(`/operations/${encodeURIComponent(code)}/products?${buildQuery({ locale, page, perPage })}`);
}

export function getExportPreview(locale = 'en', filters = {}, page = 1, perPage = 20) {
  const params = { locale, ...filters, page, perPage };
  return request(`/export/preview?${buildQuery(params)}`);
}

export async function exportProducts(format, locale = 'en', extraFilters = {}) {
  const params = { format, locale, ...extraFilters };
  const url = `${BASE}/export?${buildQuery(params)}`;
  const res = await fetch(url, { headers: { ...getAuthHeaders() } });
  if (!res.ok) throw new Error(`Export failed: HTTP ${res.status}`);
  const blob = await res.blob();
  const disposition = res.headers.get('content-disposition') || '';
  let filename = `catalog.${format}`;
  const match = disposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
  if (match) filename = match[1].replace(/['"]/g, '');
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = filename;
  link.click();
  URL.revokeObjectURL(link.href);
}

export async function downloadImportTemplate() {
  const res = await fetch(`${BASE}/admin/import/template`, {
    headers: { ...getAuthHeaders() },
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  const blob = await res.blob();
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = 'wpw-pim-import-template.xlsx';
  link.click();
  URL.revokeObjectURL(link.href);
}

export async function validateImport(file) {
  const form = new FormData();
  form.append('file', file);
  const res = await fetch(`${BASE}/admin/import/validate`, {
    method: 'POST',
    headers: { ...getAuthHeaders() },
    body: form,
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `HTTP ${res.status}`);
  }
  return res.json();
}

export async function executeImport(file) {
  const form = new FormData();
  form.append('file', file);
  const res = await fetch(`${BASE}/admin/import/execute`, {
    method: 'POST',
    headers: { ...getAuthHeaders() },
    body: form,
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `HTTP ${res.status}`);
  }
  return res.text();
}

export async function syncPhotos() {
  return request('/admin/photos/sync', {
    method: 'POST',
  });
}

export async function validatePhotos(files) {
  const form = new FormData();
  for (const file of files) {
    form.append('files', file);
  }
  const res = await fetch(`${BASE}/admin/photos/validate`, {
    method: 'POST',
    headers: { ...getAuthHeaders() },
    body: form,
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `HTTP ${res.status}`);
  }
  return res.json();
}

export async function importPhotos(files) {
  const form = new FormData();
  for (const file of files) {
    form.append('files', file);
  }
  const res = await fetch(`${BASE}/admin/photos/import`, {
    method: 'POST',
    headers: { ...getAuthHeaders() },
    body: form,
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `HTTP ${res.status}`);
  }
  return res.json();
}

export async function validateArchive(file) {
  const form = new FormData();
  form.append('archive', file);
  const res = await fetch(`${BASE}/admin/photos/archive/validate`, {
    method: 'POST',
    headers: { ...getAuthHeaders() },
    body: form,
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `HTTP ${res.status}`);
  }
  return res.json();
}

export async function importArchive(file) {
  const form = new FormData();
  form.append('archive', file);
  const res = await fetch(`${BASE}/admin/photos/archive/import`, {
    method: 'POST',
    headers: { ...getAuthHeaders() },
    body: form,
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `HTTP ${res.status}`);
  }
  return res.json();
}

// Admin Catalog CRUD
export function createSection(data) {
  return request('/admin/catalog/sections', { method: 'POST', body: JSON.stringify(data) });
}
export function updateSection(id, data) {
  return request(`/admin/catalog/sections/${id}`, { method: 'PUT', body: JSON.stringify(data) });
}
export function deleteSection(id, cascade = false) {
  return request(`/admin/catalog/sections/${id}?cascade=${cascade}`, { method: 'DELETE' });
}
export function reorderSections(items) {
  return request('/admin/catalog/sections/reorder', { method: 'PUT', body: JSON.stringify({ items }) });
}
export function createCategory(data) {
  return request('/admin/catalog/categories', { method: 'POST', body: JSON.stringify(data) });
}
export function updateCategory(id, data) {
  return request(`/admin/catalog/categories/${id}`, { method: 'PUT', body: JSON.stringify(data) });
}
export function deleteCategory(id, cascade = false) {
  return request(`/admin/catalog/categories/${id}?cascade=${cascade}`, { method: 'DELETE' });
}
export function reorderCategories(items) {
  return request('/admin/catalog/categories/reorder', { method: 'PUT', body: JSON.stringify({ items }) });
}
export function createProductGroup(data) {
  return request('/admin/catalog/product-groups', { method: 'POST', body: JSON.stringify(data) });
}
export function updateProductGroup(id, data) {
  return request(`/admin/catalog/product-groups/${id}`, { method: 'PUT', body: JSON.stringify(data) });
}
export function deleteProductGroup(id) {
  return request(`/admin/catalog/product-groups/${id}`, { method: 'DELETE' });
}
export function reorderProductGroups(items) {
  return request('/admin/catalog/product-groups/reorder', { method: 'PUT', body: JSON.stringify({ items }) });
}
export async function uploadCatalogNodeImage(nodeType, nodeId, file) {
  const form = new FormData();
  form.append('file', file);
  const res = await fetch(`${BASE}/admin/catalog/${nodeType}/${nodeId}/image`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: form,
  });
  if (!res.ok) { const t = await res.text(); throw new Error(t || `HTTP ${res.status}`); }
  return res.json();
}
export function deleteCatalogNodeImage(nodeType, nodeId) {
  return request(`/admin/catalog/${nodeType}/${nodeId}/image`, { method: 'DELETE' });
}

export function getChildrenCount(type, id) {
  return request(`/admin/catalog/${type}/${id}/children-count`);
}

// Users
export function getUsers() { return request('/users'); }
export function createUser(data) { return request('/users', { method: 'POST', body: JSON.stringify(data) }); }
export function updateUser(id, data) { return request(`/users/${id}`, { method: 'PUT', body: JSON.stringify(data) }); }
export function deleteUser(id) { return request(`/users/${id}`, { method: 'DELETE' }); }

// Roles
export function getRoles() { return request('/roles'); }
export function createRole(data) { return request('/roles', { method: 'POST', body: JSON.stringify(data) }); }
export function updateRole(id, data) { return request(`/roles/${id}`, { method: 'PUT', body: JSON.stringify(data) }); }
export function deleteRole(id) { return request(`/roles/${id}`, { method: 'DELETE' }); }

export function getPriceList(apiKey) {
  return dealerRequest('/dealer/price-list', apiKey);
}

export function getSkuMapping(apiKey) {
  return dealerRequest('/dealer/sku-mapping', apiKey);
}

export async function addSkuMapping(apiKey, toolNo, dealerSku, note = '') {
  return dealerRequest('/dealer/sku-mapping', apiKey, {
    method: 'POST',
    body: JSON.stringify({ toolNo, dealerSku, note }),
  });
}

// Product create / edit
export function createProduct(data) {
  return request('/products', { method: 'POST', body: JSON.stringify(data) });
}

export function updateProduct(id, locale, data) {
  return request(`/products/${id}?locale=${locale}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export async function uploadProductImages(productId, files) {
  const form = new FormData();
  for (const file of files) form.append('files', file);
  const res = await fetch(`${BASE}/products/${productId}/images`, {
    method: 'POST',
    headers: { ...getAuthHeaders() },
    body: form,
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `HTTP ${res.status}`);
  }
  return res.json();
}

export function deleteProductImage(productId, imageId) {
  return request(`/products/${productId}/images/${imageId}`, { method: 'DELETE' });
}

export function deleteProduct(id) {
  return request(`/products/${id}`, { method: 'DELETE' });
}

export function getProductImages(productId) {
  return request(`/products/${productId}/images`);
}

// Admin dealers
export function getDealers() {
  return request('/admin/dealers');
}

export function createDealer(data) {
  return request('/admin/dealers', { method: 'POST', body: JSON.stringify(data) });
}

export function updateDealer(id, data) {
  return request(`/admin/dealers/${id}`, { method: 'PUT', body: JSON.stringify(data) });
}

export function deleteDealer(id) {
  return request(`/admin/dealers/${id}`, { method: 'DELETE' });
}

export function resetDealerPassword(id) {
  return request(`/admin/dealers/${id}/reset-password`, { method: 'POST' });
}

// Stock price list
export function getStockPrices() { return request('/admin/price/stock'); }
export function upsertStockPrice(data) { return request('/admin/price/stock', { method: 'PUT', body: JSON.stringify(data) }); }
export function deleteStockPrice(toolNo, minQty) { return request(`/admin/price/stock/${encodeURIComponent(toolNo)}/${minQty}`, { method: 'DELETE' }); }

export async function importStockPrices(file) {
  const form = new FormData();
  form.append('file', file);
  const res = await fetch(`${BASE}/admin/price/stock/import`, {
    method: 'POST',
    headers: { ...getAuthHeaders() },
    body: form,
  });
  if (!res.ok) throw new Error(await res.text() || `HTTP ${res.status}`);
  return res.json();
}

export async function downloadStockPrices() {
  const res = await fetch(`${BASE}/admin/price/stock/export`, { headers: { ...getAuthHeaders() } });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res;
}

export async function downloadStockPriceTemplate() {
  const res = await fetch(`${BASE}/admin/price/stock/template`, { headers: { ...getAuthHeaders() } });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res;
}

// Dealer price list
export async function getDealerPriceList(dealerId) {
  const res = await fetch(`${BASE}/admin/dealers/${dealerId}/price-list`, {
    headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
  });
  if (res.status === 204) return null;
  if (res.status === 401) { localStorage.removeItem('authToken'); throw new Error('Session expired'); }
  if (!res.ok) { const text = await res.text(); throw new Error(text || `HTTP ${res.status}`); }
  return res.json();
}

export async function importDealerPriceList(dealerId, file, currencyCode, validTo) {
  const form = new FormData();
  form.append('file', file);
  const params = new URLSearchParams({ currencyCode });
  if (validTo) params.append('validTo', validTo);
  const res = await fetch(`${BASE}/admin/dealers/${dealerId}/price-list/import?${params}`, {
    method: 'POST',
    headers: { ...getAuthHeaders() },
    body: form,
  });
  if (!res.ok) throw new Error(await res.text() || `HTTP ${res.status}`);
  return res.json();
}

export function deleteDealerPriceList(dealerId) {
  return request(`/admin/dealers/${dealerId}/price-list`, { method: 'DELETE' });
}

export async function downloadDealerPriceList(dealerId) {
  const res = await fetch(`${BASE}/admin/dealers/${dealerId}/price-list/export`, { headers: { ...getAuthHeaders() } });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res;
}

export async function downloadDealerPriceTemplate() {
  const res = await fetch(`${BASE}/admin/dealers/0/price-list/template`, { headers: { ...getAuthHeaders() } });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res;
}

export function getCurrencies() { return request('/admin/price/stock/currencies'); }


export function getSystemSettings() { return request('/admin/settings'); }
export function updateSystemSettings(dto) {
  return request('/admin/settings', { method: 'PUT', body: JSON.stringify(dto) });
}
export function getSystemStats() { return request('/admin/settings/stats'); }
export function deleteAllProductMedia() {
  return request('/admin/photos/all', { method: 'DELETE' });
}

// Dealer catalog (with API key)
async function dealerRequest(path, apiKey, options = {}) {
  // если apiKey не задан — используем JWT из localStorage (стандартный request)
  if (!apiKey) return request(path, options);

  const { headers: optHeaders, ...rest } = options;
  const res = await fetch(`/api/v1${path}`, {
    headers: {
      'Content-Type': 'application/json',
      'X-Api-Key': apiKey,
      ...(optHeaders || {}),
    },
    ...rest,
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `HTTP ${res.status}`);
  }
  const ct = res.headers.get('content-type') || '';
  if (ct.includes('application/json')) return res.json();
  return res;
}

export function getDealerCategories(apiKey, locale = 'en') {
  return dealerRequest(`/categories?locale=${locale}`, apiKey);
}

export function getDealerProducts(apiKey, filters = {}) {
  const params = {
    locale: filters.locale || 'en',
    page: filters.page || 1,
    perPage: filters.perPage || 48,
    sectionId: filters.sectionId || '',
    categoryId: filters.categoryId || '',
    groupId: filters.groupId || '',
    operation: filters.operation || '',
    toolMaterial: filters.toolMaterial || '',
    workpieceMaterial: filters.workpieceMaterial || '',
    machineType: filters.machineType || '',
    machineBrand: filters.machineBrand || '',
    cuttingType: filters.cuttingType || '',
    dMmMin: filters.dMmMin || '',
    dMmMax: filters.dMmMax || '',
    shankMm: filters.shankMm || '',
    hasBallBearing: filters.hasBallBearing || '',
    productType: filters.productType || '',
    inStock: filters.inStock || '',
    priceMin: filters.priceMin || '',
    priceMax: filters.priceMax || '',
  };
  const q = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== null && v !== '') q.set(k, String(v));
  }
  return dealerRequest(`/products?${q}`, apiKey);
}

// Cart (dealer, JWT-only)
export function getCart() {
  return request('/dealer/cart');
}

// items: [{ productId, qty }]
export function addToCart(items) {
  return request('/dealer/cart/items', {
    method: 'POST',
    body: JSON.stringify({ items }),
  });
}

export function addToCartByFilter(filters = {}) {
  const params = {
    locale: filters.locale || 'en',
    sectionId: filters.sectionId || '',
    categoryId: filters.categoryId || '',
    groupId: filters.groupId || '',
    operation: filters.operation || '',
    toolMaterial: filters.toolMaterial || '',
    workpieceMaterial: filters.workpieceMaterial || '',
    machineType: filters.machineType || '',
    machineBrand: filters.machineBrand || '',
    cuttingType: filters.cuttingType || '',
    dMmMin: filters.dMmMin || '',
    dMmMax: filters.dMmMax || '',
    shankMm: filters.shankMm || '',
    hasBallBearing: filters.hasBallBearing || '',
    productType: filters.productType || '',
    inStock: filters.inStock || '',
    priceMin: filters.priceMin || '',
    priceMax: filters.priceMax || '',
  };
  const q = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== null && v !== '') q.set(k, String(v));
  }
  return request(`/dealer/cart/items/by-filter?${q}`, { method: 'POST' });
}

export function updateCartQty(productId, qty) {
  return request(`/dealer/cart/items/${productId}?qty=${qty}`, { method: 'PATCH' });
}

export function removeFromCart(productId) {
  return request(`/dealer/cart/items/${productId}`, { method: 'DELETE' });
}

export function clearCart() {
  return request('/dealer/cart', { method: 'DELETE' });
}

export function checkout(comment) {
  return request('/dealer/cart/checkout', {
    method: 'POST',
    body: JSON.stringify({ comment: comment || null }),
  });
}

// ── Dealer Orders ──────────────────────────────────────────────────────────────

export function changePassword(currentPassword, newPassword) {
  return request('/dealer/change-password', {
    method: 'POST',
    body: JSON.stringify({ currentPassword, newPassword }),
  });
}

export function getDealerOrders() {
  return request('/dealer/orders');
}

export function getDealerOrder(orderId) {
  return request(`/dealer/orders/${orderId}`);
}

// ── Admin Orders ───────────────────────────────────────────────────────────────

export function getAdminDealerOrders(dealerId) {
  return request(`/admin/dealers/${dealerId}/orders`);
}

export function getAdminOrder(orderId) {
  return request(`/admin/orders/${orderId}`);
}

export function changeOrderStatus(orderId, status) {
  return request(`/admin/orders/${orderId}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  });
}

export function hasDealerPendingOrders(dealerId) {
  return request(`/admin/dealers/${dealerId}/orders/pending`);
}

export function getPendingDealerIds() {
  return request('/admin/orders/pending-dealer-ids');
}

// ── Notification Emails ────────────────────────────────────────────────────────

export function getNotificationEmails() {
  return request('/admin/notification-emails');
}

export function createNotificationEmail(data) {
  return request('/admin/notification-emails', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function updateNotificationEmail(id, data) {
  return request(`/admin/notification-emails/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export function deleteNotificationEmail(id) {
  return request(`/admin/notification-emails/${id}`, { method: 'DELETE' });
}
