/**
 * intelligenceService.js — Trade Intelligence API.
 *
 * Covers: Cost Estimation, Market Opportunity Ranking, Government Incentives,
 * Negotiation Assistant, HS Code Classification, Full Export Analysis.
 *
 * Backend:
 *  - IntelligenceController @ /api/v1 (cost, incentives, market-opportunity, negotiation)
 *  - HsClassificationController @ /api/v1/hs (classify, candidates)
 *  - ExportAnalysisController @ /api/v1/export (analyze)
 */

import { get, post } from './httpClient';

export const intelligenceApi = {
  // ── Cost Estimation ────────────────────────────────────────────────────────

  /**
   * POST /api/v1/cost-estimation
   * @param {string} country - destination country name
   * @param {string} hsCode - HS code (no dots)
   * @param {number} productValue - declared value per unit
   * @param {number} quantity - number of units
   * @param {string} currency - e.g. "USD"
   */
  estimateCost(country, hsCode, productValue, quantity = 1, currency = 'USD') {
    return post('/v1/cost-estimation', { country, hsCode, productValue, quantity, currency });
  },

  // ── Government Incentives ──────────────────────────────────────────────────

  /**
   * GET /api/v1/incentives/{country} — incentive schemes for the country
   */
  getIncentivesByCountry(country) {
    return get(`/v1/incentives/${encodeURIComponent(country)}`);
  },

  /**
   * GET /api/v1/incentives/{country}/{hsCode} — incentives for specific HS code + country
   */
  getIncentivesByCountryAndHs(country, hsCode) {
    return get(`/v1/incentives/${encodeURIComponent(country)}/${hsCode}`);
  },

  // ── Market Opportunity ─────────────────────────────────────────────────────

  /**
   * GET /api/v1/market-opportunity/{hsCode} — rank all supported countries
   */
  rankMarketOpportunity(hsCode) {
    return get(`/v1/market-opportunity/${hsCode}`);
  },

  /**
   * POST /api/v1/market-opportunity/rank — rank specific countries for HS code
   * @param {string} hsCode
   * @param {string[]} [countries] - optional list; omit to rank all
   */
  rankMarketOpportunityForCountries(hsCode, countries) {
    return post('/v1/market-opportunity/rank', { hsCode, countries });
  },

  // ── Negotiation Assistant ──────────────────────────────────────────────────

  /**
   * POST /api/v1/ai/negotiation-assistant
   * NVIDIA-powered negotiation advice grounded in regulatory context.
   * @param {object} params - { product, hsCode, country, buyerMessage, objective, quantity, targetPrice }
   */
  negotiationAssistant(params) {
    return post('/v1/ai/negotiation-assistant', params);
  },

  // ── HS Code Classification ─────────────────────────────────────────────────

  /**
   * POST /api/v1/hs/classify — AI-assisted HS Code classification
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

  // ── Full Export Analysis ───────────────────────────────────────────────────

  /**
   * POST /api/v1/export/analyze
   * Confirmed HS code + destination → regulations + compliance + AI recommendations
   * @param {object} params - { originCountry, destinationCountry, hsCode, productName, ... }
   */
  analyzeExport(params) {
    return post('/v1/export/analyze', params);
  },
};
