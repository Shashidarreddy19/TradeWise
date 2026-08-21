package com.trade.dto.dashboard;

import lombok.Builder;
import lombok.Data;

/**
 * Dashboard summary statistics for an Exporter user.
 * Reflects the new workflow where exporters do not accept orders themselves.
 */
@Data
@Builder
public class ExporterDashboardResponse {

    private long totalProducts;

    // Requests waiting for a logistics partner to accept
    private long pendingLogisticsRequests;

    // Requests accepted by a logistics partner
    private long acceptedByLogistics;

    // Requests where all logistics partners rejected
    private long rejectedRequests;

    // Shipments currently in transit
    private long inTransit;

    // Shipments delivered
    private long delivered;
}
