package com.trade.dto.dashboard;

import lombok.Builder;
import lombok.Data;

/**
 * Dashboard summary statistics for a Logistics Partner user.
 */
@Data
@Builder
public class LogisticsDashboardResponse {

    // Requests in PENDING_LOGISTICS state (available to accept)
    private long availableRequests;

    // Shipments this partner accepted (all statuses except DELIVERED)
    private long acceptedShipments;

    // Shipments actively moving (IN_TRANSIT, AT_IMPORT_CUSTOMS, OUT_FOR_DELIVERY)
    private long activeShipments;

    // Shipments fully delivered
    private long completedDeliveries;
}
