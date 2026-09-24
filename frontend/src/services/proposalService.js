/**
 * proposalService.js — Logistics Proposal API endpoints.
 *
 * Backend: LogisticsProposalController @ /api/proposals
 */

import { get, post, patch } from './httpClient';

export const proposalApi = {
  /**
   * Submit a new logistics quote / proposal for an order.
   * POST /api/proposals
   * @param {Object} proposalData - { orderId, proposedCost, currency, estimatedTransitDays, estimatedPickupDate, estimatedDeliveryDate, offeredServices, notes }
   */
  submitProposal(proposalData) {
    return post('/proposals', proposalData);
  },

  /**
   * Get all proposals submitted for a specific order (for Exporter).
   * GET /api/proposals/order/{orderId}
   * @param {number|string} orderId
   */
  getProposalsForOrder(orderId) {
    return get(`/proposals/order/${orderId}`);
  },

  /**
   * Get all proposals submitted by the current authenticated logistics provider.
   * GET /api/proposals/my
   */
  getMyProposals() {
    return get('/proposals/my');
  },

  /**
   * Exporter accepts a logistics proposal.
   * PATCH /api/proposals/{proposalId}/accept
   * @param {number|string} proposalId
   */
  acceptProposal(proposalId) {
    return patch(`/proposals/${proposalId}/accept`, {});
  },

  /**
   * Exporter rejects a logistics proposal.
   * PATCH /api/proposals/{proposalId}/reject
   * @param {number|string} proposalId
   */
  rejectProposal(proposalId) {
    return patch(`/proposals/${proposalId}/reject`, {});
  },
};
