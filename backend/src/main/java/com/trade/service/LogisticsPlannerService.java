package com.trade.service;

import com.trade.dto.shipment.ShipmentPlanRequest;
import com.trade.dto.shipment.ShipmentPlanResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface LogisticsPlannerService {

    ShipmentPlanResponse createShipmentPlan(ShipmentPlanRequest request, String userEmail);

    ShipmentPlanResponse calculateEstimate(ShipmentPlanRequest request);

    Map<String, Object> getAiRecommendations(ShipmentPlanRequest request);

    Map<String, Object> trackShipment(String reference);

    ShipmentPlanResponse getShipmentPlanById(Long id, String userEmail);

    List<Map<String, Object>> getAllPorts();

    List<Map<String, Object>> getAllCarriers();

    List<Map<String, Object>> getAllRoutes();

    List<Map<String, Object>> getAllContainers();

    List<Map<String, Object>> getAllWarehouses();

    List<Map<String, Object>> getAllFreightRates();

    List<ShipmentPlanResponse> getShipmentHistory(String userEmail);

    Map<String, Object> calculateCostBreakdown(ShipmentPlanRequest request);

    Map<String, Object> assessRisk(ShipmentPlanRequest request);

    List<Map<String, Object>> getCustomsDocumentChecklist(String hsCode, String destinationCountry);

    Map<String, Object> getIncotermBreakdown(String code);

    Map<String, Object> getInsuranceQuote(BigDecimal cargoValue, String policyType);

    Map<String, Object> getAnalyticsDashboardData();
}
