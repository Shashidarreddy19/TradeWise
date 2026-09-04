/**
 * logisticsService.js — Enterprise Logistics Management & Shipment Planning APIs.
 *
 * Backend: LogisticsPlannerController @ /api/shipment
 */

import { get, post } from './httpClient';

export const logisticsApi = {
  createPlan(data) {
    return post('/shipment/create', data);
  },

  estimatePlan(data) {
    return post('/shipment/estimate', data);
  },

  getRecommendations(data) {
    return post('/shipment/recommend', data);
  },

  trackShipment(reference) {
    return post('/shipment/track', { reference });
  },

  getPlanById(id) {
    return get(`/shipment/${id}`);
  },

  getPorts() {
    return get('/shipment/ports');
  },

  getCarriers() {
    return get('/shipment/carriers');
  },

  getRoutes() {
    return get('/shipment/routes');
  },

  getContainers() {
    return get('/shipment/containers');
  },

  getWarehouses() {
    return get('/shipment/warehouses');
  },

  getFreightRates() {
    return get('/shipment/freight-rates');
  },

  getHistory() {
    return get('/shipment/history');
  },

  getCostBreakdown(data) {
    return post('/shipment/cost', data);
  },

  getRiskAssessment(data) {
    return post('/shipment/risk', data);
  },

  getDocuments(hsCode, destinationCountry) {
    return post('/shipment/documents', { hsCode, destinationCountry });
  },

  getIncoterm(incoterm) {
    return post('/shipment/incoterm', { incoterm });
  },

  getInsurance(cargoValue, policyType) {
    return post('/shipment/insurance', { cargoValue, policyType });
  },

  getAnalytics() {
    return post('/shipment/analytics', {});
  },
};
