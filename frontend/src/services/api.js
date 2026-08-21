/**
 * Centralized API service layer for the Trade Platform frontend.
 *
 * - Base URL points to the Spring Boot backend (localhost:8081)
 * - Automatically attaches JWT Bearer token from localStorage
 * - Provides typed wrappers for every backend endpoint
 * - Handles 401 (expired token) by clearing auth and redirecting to /login
 */

const BASE_URL = 'http://localhost:8081/api';

// ── Token helpers ────────────────────────────────────────────────────────────

export function getToken() {
  return localStorage.getItem('trade_token') || sessionStorage.getItem('trade_token');
}

export function setToken(token) {
  localStorage.setItem('trade_token', token);
}

export function clearAuth() {
  localStorage.removeItem('trade_token');
  localStorage.removeItem('trade_user');
  sessionStorage.removeItem('trade_token');
  sessionStorage.removeItem('trade_user');
}

export function getUser() {
  const raw = localStorage.getItem('trade_user') || sessionStorage.getItem('trade_user');
  if (!raw) return null;
  try { return JSON.parse(raw); } catch { return null; }
}

export function setUser(user) {
  localStorage.setItem('trade_user', JSON.stringify(user));
}

export function isAuthenticated() {
  return !!getToken();
}

// ── Core fetch wrapper ───────────────────────────────────────────────────────

async function request(endpoint, options = {}) {
  const url = `${BASE_URL}${endpoint}`;
  const token = getToken();

  const headers = {
    'Content-Type': 'application/json',
    ...(token && { Authorization: `Bearer ${token}` }),
    ...options.headers,
  };

  const config = {
    ...options,
    headers,
  };

  const response = await fetch(url, config);

  // Handle 401 — token expired or invalid
  if (response.status === 401) {
    clearAuth();
    window.location.href = '/login';
    throw new Error('Session expired. Please log in again.');
  }

  // Handle 403 — forbidden
  if (response.status === 403) {
    throw new Error('Access denied. You do not have permission for this action.');
  }

  // Parse JSON response
  const data = await response.json().catch(() => null);

  if (!response.ok) {
    const message = data?.message || data?.error || `Request failed (${response.status})`;
    const error = new Error(message);
    error.status = response.status;
    error.data = data;
    // Flag quota exhaustion for ML/AI features
    if (message.toLowerCase().includes('quota') || message.toLowerCase().includes('rate limit') || response.status === 429) {
      error.isQuotaExhausted = true;
      error.message = 'API quota exhausted. The service will resume shortly. Using cached data in the meantime.';
    }
    throw error;
  }

  return data;
}

// ── HTTP method shortcuts ────────────────────────────────────────────────────

function get(endpoint) {
  return request(endpoint, { method: 'GET' });
}

function post(endpoint, body) {
  return request(endpoint, { method: 'POST', body: JSON.stringify(body) });
}

function put(endpoint, body) {
  return request(endpoint, { method: 'PUT', body: JSON.stringify(body) });
}

function patch(endpoint, body) {
  return request(endpoint, { method: 'PATCH', body: JSON.stringify(body) });
}

function del(endpoint) {
  return request(endpoint, { method: 'DELETE' });
}

// ═══════════════════════════════════════════════════════════════════════════════
// AUTH endpoints
// ═══════════════════════════════════════════════════════════════════════════════

export const authApi = {
  /**
   * POST /api/auth/register
   * @param {object} data - RegisterRequest fields
   * @returns {Promise<{success, message, data: AuthResponse}>}
   */
  register(data) {
    return post('/auth/register', data);
  },

  /**
   * POST /api/auth/login
   * @param {{email: string, password: string}} credentials
   * @returns {Promise<{success, message, data: AuthResponse}>}
   */
  login(credentials) {
    return post('/auth/login', credentials);
  },

  /**
   * GET /api/auth/profile
   * @returns {Promise<{success, data: UserProfileResponse}>}
   */
  getProfile() {
    return get('/auth/profile');
  },

  /**
   * GET /api/auth/check-email?email=...
   * Real-time duplicate detection (debounced on frontend).
   */
  checkEmail(email) {
    return get(`/auth/check-email?email=${encodeURIComponent(email)}`);
  },

  /**
   * GET /api/auth/check-phone?phone=...
   * Real-time duplicate detection (debounced on frontend).
   */
  checkPhone(phone) {
    return get(`/auth/check-phone?phone=${encodeURIComponent(phone)}`);
  },
};

// ═══════════════════════════════════════════════════════════════════════════════
// PRODUCTS endpoints (EXPORTER)
// ═══════════════════════════════════════════════════════════════════════════════

export const productsApi = {
  /**
   * GET /api/products — list all products for authenticated exporter
   */
  getAll() {
    return get('/products');
  },

  /**
   * GET /api/products/:id
   */
  getById(id) {
    return get(`/products/${id}`);
  },

  /**
   * POST /api/products — create a new product
   * @param {object} product - ProductRequest fields
   */
  create(product) {
    return post('/products', product);
  },

  /**
   * PUT /api/products/:id — update an existing product
   */
  update(id, product) {
    return put(`/products/${id}`, product);
  },

  /**
   * DELETE /api/products/:id
   */
  delete(id) {
    return del(`/products/${id}`);
  },
};

// ═══════════════════════════════════════════════════════════════════════════════
// ORDERS endpoints (shared by EXPORTER and LOGISTICS)
// ═══════════════════════════════════════════════════════════════════════════════

export const ordersApi = {
  // ── Exporter ───────────────────────────────────────────────────────────────

  /**
   * GET /api/orders — list exporter's own orders
   */
  getMyOrders() {
    return get('/orders');
  },

  /**
   * GET /api/orders/:id
   */
  getById(id) {
    return get(`/orders/${id}`);
  },

  /**
   * POST /api/orders — create a new shipment request
   * @param {object} order - OrderRequest fields
   */
  create(order) {
    return post('/orders', order);
  },

  /**
   * PUT /api/orders/:id — update (only while PENDING_LOGISTICS)
   */
  update(id, order) {
    return put(`/orders/${id}`, order);
  },

  // ── Logistics ──────────────────────────────────────────────────────────────

  /**
   * GET /api/orders/pending — all PENDING_LOGISTICS requests (excluding own rejections)
   */
  getPending() {
    return get('/orders/pending');
  },

  /**
   * PATCH /api/orders/:id/accept — accept a shipment request
   */
  accept(id) {
    return patch(`/orders/${id}/accept`);
  },

  /**
   * PATCH /api/orders/:id/reject — reject a shipment request
   */
  reject(id) {
    return patch(`/orders/${id}/reject`);
  },
};

// ═══════════════════════════════════════════════════════════════════════════════
// SHIPMENTS endpoints (LOGISTICS)
// ═══════════════════════════════════════════════════════════════════════════════

export const shipmentsApi = {
  /**
   * GET /api/shipments — list all shipments for current logistics partner
   */
  getAll() {
    return get('/shipments');
  },

  /**
   * GET /api/shipments/:id
   */
  getById(id) {
    return get(`/shipments/${id}`);
  },

  /**
   * PATCH /api/shipments/:id/status — update shipment status
   * @param {string} shipmentStatus - one of ShipmentStatus enum values
   */
  updateStatus(id, shipmentStatus) {
    return patch(`/shipments/${id}/status`, { shipmentStatus });
  },
};

// ═══════════════════════════════════════════════════════════════════════════════
// DASHBOARD endpoints
// ═══════════════════════════════════════════════════════════════════════════════

export const dashboardApi = {
  /**
   * GET /api/dashboard/exporter
   */
  getExporterDashboard() {
    return get('/dashboard/exporter');
  },

  /**
   * GET /api/dashboard/logistics
   */
  getLogisticsDashboard() {
    return get('/dashboard/logistics');
  },
};

// ═══════════════════════════════════════════════════════════════════════════════
// MARKET ANALYSIS endpoint (EXPORTER)
// ═══════════════════════════════════════════════════════════════════════════════

export const marketApi = {
  /**
   * POST /api/market-analysis
   * @param {{countryId: number, productId: number}} params
   */
  analyze(params) {
    return post('/market-analysis', params);
  },
};

// ═══════════════════════════════════════════════════════════════════════════════
// REFERENCE DATA endpoints (public, no auth required)
// ═══════════════════════════════════════════════════════════════════════════════

export const referenceApi = {
  /**
   * GET /api/countries
   */
  getCountries() {
    return get('/countries');
  },

  /**
   * GET /api/categories
   */
  getCategories() {
    return get('/categories');
  },
};



// ═══════════════════════════════════════════════════════════════════════════════
// REGULATORY INTELLIGENCE endpoints (reads from TradeData via backend)
// ═══════════════════════════════════════════════════════════════════════════════

export const regulatoryApi = {
  /** GET /api/v1/regulations/{country}/{hsCode} — Full regulatory profile */
  getRegulations(country, hsCode) { return get(`/v1/regulations/${encodeURIComponent(country)}/${hsCode}`); },

  /** GET /api/v1/compliance/{country}/{hsCode} — Compliance score */
  getCompliance(country, hsCode) { return get(`/v1/compliance/${encodeURIComponent(country)}/${hsCode}`); },

  /** GET /api/v1/documents/{country}/{hsCode} */
  getDocuments(country, hsCode) { return get(`/v1/documents/${encodeURIComponent(country)}/${hsCode}`); },

  /** GET /api/v1/certificates/{country}/{hsCode} */
  getCertificates(country, hsCode) { return get(`/v1/certificates/${encodeURIComponent(country)}/${hsCode}`); },

  /** GET /api/v1/requirements/{country}/{hsCode} — Aggregate requirements */
  getRequirements(country, hsCode) { return get(`/v1/requirements/${encodeURIComponent(country)}/${hsCode}`); },

  /** GET /api/v1/hs/search?query=...&country=...&page=0&size=20 */
  searchHs(query, country, page = 0, size = 20) {
    let url = `/v1/hs/search?query=${encodeURIComponent(query)}&page=${page}&size=${size}`;
    if (country) url += `&country=${encodeURIComponent(country)}`;
    return get(url);
  },

  /** GET /api/v1/hs/{hsCode} */
  getHsCode(hsCode) { return get(`/v1/hs/${hsCode}`); },

  /** GET /api/v1/countries — Supported regulatory countries */
  getCountries() { return get('/v1/countries'); },

  /** GET /api/v1/dashboard/statistics — Regulatory DB stats */
  getDashboardStatistics() { return get('/v1/dashboard/statistics'); },
};


// ═══════════════════════════════════════════════════════════════════════════════
// AI REGULATORY INTELLIGENCE endpoints
// ═══════════════════════════════════════════════════════════════════════════════

export const aiApi = {
  /** POST /api/v1/ai/regulatory-chat — AI-grounded regulatory Q&A */
  regulatoryChat(country, hsCode, question) {
    return post('/v1/ai/regulatory-chat', { country, hsCode, question });
  },

  /** POST /api/v1/ai/summarize-document — Document summarisation */
  summarizeDocument(text, title) {
    return post('/v1/ai/summarize-document', { text, title });
  },

  /** GET /api/v1/ai/status — AI service availability */
  getStatus() {
    return get('/v1/ai/status');
  },

  /** GET /api/v1/export-guide/{country}/{hsCode} — Step-by-step export guide */
  getExportGuide(country, hsCode) {
    return get(`/v1/export-guide/${encodeURIComponent(country)}/${hsCode}`);
  },
};


// ═══════════════════════════════════════════════════════════════════════════════
// INTELLIGENCE endpoints — Cost Estimation, Market Opportunity,
// Government Incentives, Negotiation Assistant
// All backed by real TradeData — no mock/fabricated responses.
// ═══════════════════════════════════════════════════════════════════════════════

export const intelligenceApi = {
  /**
   * POST /api/v1/cost-estimation
   * @param {string} country - destination country name
   * @param {string} hsCode  - HS code (no dots)
   * @param {number} productValue - declared value per unit
   * @param {number} quantity
   * @param {string} currency - e.g. "USD"
   */
  estimateCost(country, hsCode, productValue, quantity = 1, currency = 'USD') {
    return post('/v1/cost-estimation', { country, hsCode, productValue, quantity, currency });
  },

  /**
   * GET /api/v1/incentives/{country}
   * Returns real government incentive schemes for the country.
   */
  getIncentivesByCountry(country) {
    return get(`/v1/incentives/${encodeURIComponent(country)}`);
  },

  /**
   * GET /api/v1/incentives/{country}/{hsCode}
   * Returns incentives applicable to the specific HS code + country.
   */
  getIncentivesByCountryAndHs(country, hsCode) {
    return get(`/v1/incentives/${encodeURIComponent(country)}/${hsCode}`);
  },

  /**
   * GET /api/v1/market-opportunity/{hsCode}
   * Ranks all supported countries for this product's HS code.
   */
  rankMarketOpportunity(hsCode) {
    return get(`/v1/market-opportunity/${hsCode}`);
  },

  /**
   * POST /api/v1/market-opportunity/rank
   * Rank specific countries for this HS code.
   * @param {string} hsCode
   * @param {string[]} [countries] - optional list; omit to rank all
   */
  rankMarketOpportunityForCountries(hsCode, countries) {
    return post('/v1/market-opportunity/rank', { hsCode, countries });
  },

  /**
   * POST /api/v1/ai/negotiation-assistant
   * NVIDIA-powered negotiation advice grounded in real regulatory context.
   * @param {object} params - { product, hsCode, country, buyerMessage, objective, quantity, targetPrice }
   */
  negotiationAssistant(params) {
    return post('/v1/ai/negotiation-assistant', params);
  },

  /**
   * POST /api/v1/hs/classify
   * AI-assisted HS Code Classification for Indian exports.
   * @param {object} product - { productName, category, description, material, composition, function, manufacturingProcess, physicalForm, specifications, originCountry }
   */
  classifyHsCode(product) {
    return post('/v1/hs/classify', { ...product, originCountry: 'India' });
  },

  /**
   * GET /api/v1/hs/candidates?product=...&category=...&material=...
   * Quick HS candidate lookup without full AI ranking.
   */
  getHsCandidates(product, category, material) {
    let url = `/v1/hs/candidates?product=${encodeURIComponent(product)}`;
    if (category) url += `&category=${encodeURIComponent(category)}`;
    if (material) url += `&material=${encodeURIComponent(material)}`;
    return get(url);
  },

  /**
   * POST /api/v1/export/analyze
   * Full export analysis: confirmed HS code + destination → regulations + compliance + AI
   */
  analyzeExport(params) {
    return post('/v1/export/analyze', params);
  },
};
