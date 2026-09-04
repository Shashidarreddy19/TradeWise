/**
 * regulatoryService.js — Regulatory Intelligence API.
 *
 * Backend: RegulatoryController @ /api/v1
 * Reads from TradeData DB (HS codes, regulations, compliance).
 */

import { get } from './httpClient';

export const regulatoryApi = {
  /**
   * GET /api/v1/regulations/{country}/{hsCode} — Full regulatory profile
   */
  getRegulations(country, hsCode) {
    return get(`/v1/regulations/${encodeURIComponent(country)}/${hsCode}`);
  },

  /**
   * GET /api/v1/compliance/{country}/{hsCode} — Compliance score
   */
  getCompliance(country, hsCode) {
    return get(`/v1/compliance/${encodeURIComponent(country)}/${hsCode}`);
  },

  /**
   * GET /api/v1/documents/{country}/{hsCode} — Required documents
   */
  getDocuments(country, hsCode) {
    return get(`/v1/documents/${encodeURIComponent(country)}/${hsCode}`);
  },

  /**
   * GET /api/v1/certificates/{country}/{hsCode} — Certifications
   */
  getCertificates(country, hsCode) {
    return get(`/v1/certificates/${encodeURIComponent(country)}/${hsCode}`);
  },

  /**
   * GET /api/v1/requirements/{country}/{hsCode} — Aggregate requirements
   */
  getRequirements(country, hsCode) {
    return get(`/v1/requirements/${encodeURIComponent(country)}/${hsCode}`);
  },

  /**
   * GET /api/v1/hs/search?query=...&country=...&page=0&size=20 — Search HS codes
   */
  searchHs(query, country, page = 0, size = 20) {
    let url = `/v1/hs/search?query=${encodeURIComponent(query)}&page=${page}&size=${size}`;
    if (country) url += `&country=${encodeURIComponent(country)}`;
    return get(url);
  },

  /**
   * GET /api/v1/hs/{hsCode} — Single HS code lookup
   */
  getHsCode(hsCode) {
    return get(`/v1/hs/${hsCode}`);
  },

  /**
   * GET /api/v1/countries — Supported regulatory countries from TradeData
   */
  getCountries() {
    return get('/v1/countries');
  },

  /**
   * GET /api/v1/dashboard/statistics — Regulatory DB coverage stats
   */
  getDashboardStatistics() {
    return get('/v1/dashboard/statistics');
  },
};
