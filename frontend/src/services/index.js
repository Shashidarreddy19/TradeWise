/**
 * services/index.js — Barrel export for all API service modules.
 *
 * Import everything from here:
 *   import { authApi, productsApi, getToken, clearAuth, ... } from '../services';
 *
 * Service files:
 *   httpClient.js        — Core fetch wrapper, token helpers, auth state
 *   authService.js       — Login, register, profile, email/phone check
 *   productService.js    — Products CRUD (Exporter)
 *   orderService.js      — Orders (Exporter + Logistics)
 *   shipmentService.js   — Shipments (Logistics)
 *   dashboardService.js  — Dashboard statistics
 *   regulatoryService.js — Regulatory intelligence (TradeData)
 *   aiService.js         — AI regulatory chat, summarize, export guide
 *   intelligenceService.js — Cost estimation, market opportunity, incentives, negotiation, HS classification
 *   referenceService.js  — Countries, categories, market analysis
 */

// Core utilities (token management, auth state)
export {
  getToken,
  setToken,
  getUser,
  setUser,
  clearAuth,
  isAuthenticated,
  BASE_URL,
} from './httpClient';

// Auth
export { authApi } from './authService';

// Products
export { productsApi } from './productService';

// Orders
export { ordersApi } from './orderService';

// Shipments
export { shipmentsApi } from './shipmentService';

// Dashboard
export { dashboardApi } from './dashboardService';

// Regulatory Intelligence
export { regulatoryApi } from './regulatoryService';

// AI Services
export { aiApi } from './aiService';

// Trade Intelligence (cost, incentives, ML ranking, negotiation, HS classification, export analysis)
export { intelligenceApi } from './intelligenceService';

// Reference Data & Market Analysis
export { referenceApi, marketApi } from './referenceService';

// Enterprise Logistics Planner
export { logisticsApi } from './logisticsService';

