package com.trade.controller;

import com.trade.dto.ApiResponse;
import com.trade.dto.shipment.ShipmentPlanRequest;
import com.trade.dto.shipment.ShipmentPlanResponse;
import com.trade.service.LogisticsPlannerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shipment")
@RequiredArgsConstructor
public class LogisticsPlannerController {

    private final LogisticsPlannerService logisticsPlannerService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<ShipmentPlanResponse>> createShipment(
            @Valid @RequestBody ShipmentPlanRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails != null ? userDetails.getUsername() : "anonymous@tradewise.com";
        ShipmentPlanResponse response = logisticsPlannerService.createShipmentPlan(request, email);
        return ResponseEntity.ok(ApiResponse.success("Shipment plan generated successfully", response));
    }

    @PostMapping("/estimate")
    public ResponseEntity<ApiResponse<ShipmentPlanResponse>> estimateShipment(
            @Valid @RequestBody ShipmentPlanRequest request) {
        ShipmentPlanResponse response = logisticsPlannerService.calculateEstimate(request);
        return ResponseEntity.ok(ApiResponse.success("Shipment estimate calculated", response));
    }

    @PostMapping("/recommend")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRecommendations(
            @Valid @RequestBody ShipmentPlanRequest request) {
        Map<String, Object> response = logisticsPlannerService.getAiRecommendations(request);
        return ResponseEntity.ok(ApiResponse.success("AI logistics recommendations retrieved", response));
    }

    @PostMapping("/track")
    public ResponseEntity<ApiResponse<Map<String, Object>>> trackShipment(
            @RequestBody Map<String, String> body) {
        String ref = body.getOrDefault("reference", body.getOrDefault("trackingNumber", "TW-PLN-847291"));
        Map<String, Object> tracking = logisticsPlannerService.trackShipment(ref);
        return ResponseEntity.ok(ApiResponse.success("Live tracking data retrieved", tracking));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShipmentPlanResponse>> getShipmentById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails != null ? userDetails.getUsername() : "anonymous@tradewise.com";
        ShipmentPlanResponse response = logisticsPlannerService.getShipmentPlanById(id, email);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/ports")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPorts() {
        return ResponseEntity.ok(ApiResponse.success(logisticsPlannerService.getAllPorts()));
    }

    @GetMapping("/carriers")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCarriers() {
        return ResponseEntity.ok(ApiResponse.success(logisticsPlannerService.getAllCarriers()));
    }

    @GetMapping("/routes")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRoutes() {
        return ResponseEntity.ok(ApiResponse.success(logisticsPlannerService.getAllRoutes()));
    }

    @GetMapping("/containers")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getContainers() {
        return ResponseEntity.ok(ApiResponse.success(logisticsPlannerService.getAllContainers()));
    }

    @GetMapping("/warehouses")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getWarehouses() {
        return ResponseEntity.ok(ApiResponse.success(logisticsPlannerService.getAllWarehouses()));
    }

    @GetMapping("/freight-rates")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFreightRates() {
        return ResponseEntity.ok(ApiResponse.success(logisticsPlannerService.getAllFreightRates()));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<ShipmentPlanResponse>>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails != null ? userDetails.getUsername() : "anonymous@tradewise.com";
        return ResponseEntity.ok(ApiResponse.success(logisticsPlannerService.getShipmentHistory(email)));
    }

    @PostMapping("/cost")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCostBreakdown(
            @Valid @RequestBody ShipmentPlanRequest request) {
        return ResponseEntity.ok(ApiResponse.success(logisticsPlannerService.calculateCostBreakdown(request)));
    }

    @PostMapping("/risk")
    public ResponseEntity<ApiResponse<Map<String, Object>>> assessRisk(
            @Valid @RequestBody ShipmentPlanRequest request) {
        return ResponseEntity.ok(ApiResponse.success(logisticsPlannerService.assessRisk(request)));
    }

    @PostMapping("/documents")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDocuments(
            @RequestBody Map<String, String> body) {
        String hsCode = body.getOrDefault("hsCode", "0910.30");
        String country = body.getOrDefault("destinationCountry", "Germany");
        return ResponseEntity.ok(ApiResponse.success(logisticsPlannerService.getCustomsDocumentChecklist(hsCode, country)));
    }

    @PostMapping("/incoterm")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getIncotermDetails(
            @RequestBody Map<String, String> body) {
        String incoterm = body.getOrDefault("incoterm", "CIF");
        return ResponseEntity.ok(ApiResponse.success(logisticsPlannerService.getIncotermBreakdown(incoterm)));
    }

    @PostMapping("/insurance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInsurance(
            @RequestBody Map<String, Object> body) {
        BigDecimal val = body.containsKey("cargoValue") ? new BigDecimal(body.get("cargoValue").toString()) : BigDecimal.valueOf(2500000);
        String policy = (String) body.getOrDefault("policyType", "MARINE_CARGO_ICC_A");
        return ResponseEntity.ok(ApiResponse.success(logisticsPlannerService.getInsuranceQuote(val, policy)));
    }

    @PostMapping("/analytics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalytics() {
        return ResponseEntity.ok(ApiResponse.success(logisticsPlannerService.getAnalyticsDashboardData()));
    }
}
