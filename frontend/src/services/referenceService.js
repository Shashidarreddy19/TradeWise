/**
 * referenceService.js — Reference Data & Market Analysis API.
 *
 * Backend:
 *  - CountryController @ /api/countries
 *  - CategoryController @ /api/categories
 *  - MarketAnalysisController @ /api/market-analysis
 */

import { get, post } from './httpClient';

export const referenceApi = {
  /**
   * GET /api/countries — list active countries
   */
  getCountries() {
    return get('/countries');
  },

  /**
   * POST /api/countries/ensure — ensure a country exists and retrieve its record
   */
  ensureCountry(name, code, currency) {
    return post('/countries/ensure', { name, code, currency });
  },

  /**
   * GET /api/categories — list product categories
   */
  getCategories() {
    return get('/categories');
  },
};


export const marketApi = {
  /**
   * POST /api/market-analysis
   * @param {{countryId: number, productId: number}} params
   */
  analyze(params) {
    return post('/market-analysis', params);
  },
};
