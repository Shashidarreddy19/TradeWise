package com.trade.service;

import com.trade.dto.dashboard.ExporterDashboardResponse;
import com.trade.dto.dashboard.LogisticsDashboardResponse;

/**
 * Contract for dashboard summary statistics.
 */
public interface DashboardService {

    ExporterDashboardResponse getExporterDashboard(String exporterEmail);

    LogisticsDashboardResponse getLogisticsDashboard(String partnerEmail);
}
