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
   * GET /api/shipments/:id
   */
  getById(id) {
    return get(`/shipments/${id}`);
  },

  /**
   * PATCH /api/shipments/:id/status — update shipment status
   * @param {string} shipmentStatus - one of: ASSIGNED, PICKED_UP, IN_TRANSIT, CUSTOMS_HOLD, OUT_FOR_DELIVERY, DELIVERED
   */
  updateStatus(id, shipmentStatus) {
    return patch(`/shipments/${id}/status`, { shipmentStatus });
  },
};
