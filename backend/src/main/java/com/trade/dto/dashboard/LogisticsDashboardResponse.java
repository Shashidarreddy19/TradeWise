package com.trade.dto.dashboard;

import lombok.Builder;
import lombok.Data;

/**
 * Dashboard summary statistics for a Logistics Partner user.
 */
@Data
@Builder
public class LogisticsDashboardResponse {

    // Requests in PENDING_LOGISTICS state (available to quote / accept)
    private long totalAvailableOrders;
    private long availableRequests;

    // Quotes / Proposals
    private long pendingQuotes;
    private long acceptedQuotes;

    // Shipments
    private long pendingRequests;
    private long acceptedShipments;
    private long activeShipments;
    private long completedShipments;
    private long completedDeliveries;
}
