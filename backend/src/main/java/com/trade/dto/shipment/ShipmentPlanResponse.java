package com.trade.dto.shipment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentPlanResponse {

    private Long id;
    private String planReference;
    private String hsCode;
    private String productName;
    private String originLocation;
    private String destinationCountry;
    private String destinationCity;
    private String pickupAddress;
    private String incoterm;
    private Integer quantity;
    private BigDecimal totalWeightKg;
    private BigDecimal totalVolumeCbm;
    private Boolean isDangerousGoods;
    private String temperatureRequirement;

    // Transport Mode Recommendation
    private String recommendedMode;
    private List<Map<String, Object>> alternativeModes;

    // Container Recommendation
    private Map<String, Object> containerRecommendation;

    // Shipping Route Optimization
    private Map<String, Object> primaryRoute;
    private List<Map<String, Object>> alternativeRoutes;

    // Freight Rates & Breakdown
    private Map<String, Object> costBreakdown;
    private BigDecimal grandTotal;
    private BigDecimal costPerKg;
    private BigDecimal costPerUnit;

    // Port Intelligence
    private Map<String, Object> originPortDetails;
    private Map<String, Object> destinationPortDetails;

    // Carrier Recommendation
    private Map<String, Object> recommendedCarrier;
    private List<Map<String, Object>> carrierComparison;

    // Customs Workflow & Documents
    private List<Map<String, Object>> requiredDocuments;
    private Map<String, Object> customsChecklist;

    // Risk Assessment
    private BigDecimal overallRiskScore;
    private String riskCategory; // LOW, MODERATE, HIGH, CRITICAL
    private Map<String, Object> riskBreakdown;
    private List<String> mitigationSuggestions;

    // Incoterm Engine Breakdown
    private Map<String, Object> incotermDetails;

    // Insurance Engine
    private Map<String, Object> recommendedInsurance;

    // AI Logistics Recommendations
    private Map<String, Object> aiRecommendation;

    // Timeline & Emissions
    private Integer estimatedTransitDays;
    private LocalDate expectedDispatchDate;
    private LocalDate estimatedDeliveryDate;
    private BigDecimal carbonEmissionsKg;
    private List<Map<String, Object>> workflowMilestones;
}
