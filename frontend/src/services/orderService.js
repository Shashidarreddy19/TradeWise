/**
 * orderService.js — Orders API (shared by Exporter and Logistics roles).
 *
 * Backend: OrderController @ /api/orders
 */

import { get, post, put, patch } from './httpClient';

export const ordersApi = {
  // ── Exporter endpoints ─────────────────────────────────────────────────────

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
   * @param {object} order - { productId, destinationCountryId, quantity, ... }
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

  // ── Logistics endpoints ────────────────────────────────────────────────────

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
