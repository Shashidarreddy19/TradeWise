/**
 * shipmentService.js — Shipments API (Logistics role).
 *
 * Backend: ShipmentController @ /api/shipments
 */

import { get, patch } from './httpClient';

export const shipmentsApi = {
  /**
   * GET /api/shipments — list all shipments for current logistics partner
   */
  getAll() {
    return get('/shipments');
  },

  /**
   * GET /api/shipments/exporter — list all shipments for current exporter
   */
  getExporterShipments() {
    return get('/shipments/exporter');
  },

  /**
   * GET /api/shipments/:id
   */
  getById(id) {
    return get(`/shipments/${id}`);
  },

  /**
   * GET /api/shipments/order/:orderId — get shipment linked to an order
   */
  getByOrderId(orderId) {
    return get(`/shipments/order/${orderId}`);
  },

  /**
   * PATCH /api/shipments/:id/status — update shipment status & log tracking milestone
   * @param {number|string} id
   * @param {string|object} statusOrPayload - status string or { shipmentStatus, location, description }
   */
  updateStatus(id, statusOrPayload) {
    const payload = typeof statusOrPayload === 'string'
      ? { shipmentStatus: statusOrPayload }
      : statusOrPayload;
    return patch(`/shipments/${id}/status`, payload);
  },
};

