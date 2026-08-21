package com.trade.controller;

import com.trade.dto.ApiResponse;
import com.trade.dto.dashboard.ExporterDashboardResponse;
import com.trade.dto.dashboard.LogisticsDashboardResponse;
import com.trade.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Returns summary statistics for each dashboard type.
 *
 * GET /api/dashboard/exporter   – requires EXPORTER role
 * GET /api/dashboard/logistics  – requires LOGISTICS role
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/exporter")
    @PreAuthorize("hasRole('EXPORTER')")
    public ResponseEntity<ApiResponse<ExporterDashboardResponse>> getExporterDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {

        ExporterDashboardResponse dashboard =
                dashboardService.getExporterDashboard(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    @GetMapping("/logistics")
    @PreAuthorize("hasRole('LOGISTICS')")
    public ResponseEntity<ApiResponse<LogisticsDashboardResponse>> getLogisticsDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {

        LogisticsDashboardResponse dashboard =
                dashboardService.getLogisticsDashboard(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }
}
