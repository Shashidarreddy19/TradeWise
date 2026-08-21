package com.trade.service;

import com.trade.dto.order.OrderRequest;
import com.trade.dto.order.OrderResponse;

import java.util.List;

/**
 * Contract for Export Shipment Request management.
 *
 * Exporters: create, update, view their own requests.
 * Logistics: view pending requests, accept, reject.
 */
public interface OrderService {

    // ── Exporter operations ──────────────────────────────────────────────────

    /** Returns all requests created by this exporter. */
    List<OrderResponse> getAllOrdersForExporter(String exporterEmail);

    /** Returns a single request — exporter must own it. */
    OrderResponse getOrderById(Long id, String exporterEmail);

    /** Creates a new shipment request with status PENDING_LOGISTICS. */
    OrderResponse createOrder(OrderRequest request, String exporterEmail);

    /** Updates a request — only allowed while status is PENDING_LOGISTICS. */
    OrderResponse updateOrder(Long id, OrderRequest request, String exporterEmail);

    // ── Logistics operations ─────────────────────────────────────────────────

    /** Returns all requests with status PENDING_LOGISTICS, excluding those already rejected by this partner. */
    List<OrderResponse> getPendingRequests(String logisticsPartnerEmail);

    /**
     * Logistics partner accepts a request.
     * Changes status to LOGISTICS_ACCEPTED, saves partner, auto-creates Shipment.
     */
    OrderResponse acceptRequest(Long id, String logisticsPartnerEmail);

    /**
     * Logistics partner rejects a request.
     * Records the rejection. Request stays PENDING_LOGISTICS for other partners.
     * If all registered logistics partners have rejected, status becomes REJECTED.
     */
    OrderResponse rejectRequest(Long id, String logisticsPartnerEmail);
}
