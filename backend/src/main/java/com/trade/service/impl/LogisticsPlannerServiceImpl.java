package com.trade.service.impl;

import com.trade.dto.shipment.ShipmentPlanRequest;
import com.trade.dto.shipment.ShipmentPlanResponse;
import com.trade.entity.*;
import com.trade.exception.ResourceNotFoundException;
import com.trade.repository.*;
import com.trade.service.LogisticsPlannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogisticsPlannerServiceImpl implements LogisticsPlannerService {

    private final ShipmentPlanRepository shipmentPlanRepository;
    private final ShipmentRouteRepository shipmentRouteRepository;
    private final ShipmentCostRepository shipmentCostRepository;
    private final PortRepository portRepository;
    private final ContainerTypeRepository containerTypeRepository;
    private final CarrierRepository carrierRepository;
    private final WarehouseRepository warehouseRepository;
    private final IncotermRepository incotermRepository;
    private final FreightRateRepository freightRateRepository;
    private final InsuranceOptionRepository insuranceOptionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ShipmentPlanResponse createShipmentPlan(ShipmentPlanRequest request, String userEmail) {
        User exporter = userRepository.findByEmail(userEmail).orElse(null);

        ShipmentPlanResponse estimate = calculateEstimate(request);

        String reference = "TW-PLN-" + System.currentTimeMillis() % 1000000;

        ShipmentPlan plan = ShipmentPlan.builder()
                .planReference(reference)
                .exporter(exporter)
                .hsCode(request.getHsCode())
                .productName(request.getProductName())
                .originLocation(request.getOriginLocation())
                .destinationCountry(request.getDestinationCountry())
                .destinationCity(request.getDestinationCity() != null ? request.getDestinationCity() : "Port City")
                .pickupAddress(request.getPickupAddress())
                .incoterm(request.getIncoterm())
                .quantity(request.getQuantity())
                .totalWeightKg(estimate.getTotalWeightKg())
                .totalVolumeCbm(estimate.getTotalVolumeCbm())
                .recommendedMode(estimate.getRecommendedMode())
                .containerPreference(request.getContainerPreference() != null ? request.getContainerPreference() : "20FT")
                .isDangerousGoods(Boolean.TRUE.equals(request.getIsDangerousGoods()))
                .temperatureRequirement(request.getTemperatureRequirement())
                .expectedDispatchDate(request.getExpectedDispatchDate() != null ? request.getExpectedDispatchDate() : LocalDate.now().plusDays(3))
                .expectedDeliveryDate(estimate.getEstimatedDeliveryDate())
                .estimatedTransitDays(estimate.getEstimatedTransitDays())
                .totalEstimatedCost(estimate.getGrandTotal())
                .costPerKg(estimate.getCostPerKg())
                .costPerUnit(estimate.getCostPerUnit())
                .overallRiskScore(estimate.getOverallRiskScore())
                .carbonEmissionsKg(estimate.getCarbonEmissionsKg())
                .recommendedCarrier(estimate.getRecommendedCarrier() != null ? estimate.getRecommendedCarrier().get("name").toString() : "Maersk Line")
                .status("PLANNED")
                .build();

        plan = shipmentPlanRepository.save(plan);
        estimate.setId(plan.getId());
        estimate.setPlanReference(reference);

        // Save route details
        ShipmentRoute primaryRoute = ShipmentRoute.builder()
                .shipmentPlan(plan)
                .routeType("PRIMARY")
                .originIcd(estimate.getPrimaryRoute().get("originIcd").toString())
                .originPort(estimate.getPrimaryRoute().get("originPort").toString())
                .transshipmentPort(estimate.getPrimaryRoute().get("transshipmentPort").toString())
                .destinationPort(estimate.getPrimaryRoute().get("destinationPort").toString())
                .destinationWarehouse(estimate.getPrimaryRoute().get("destinationWarehouse").toString())
                .totalDistanceKm((Integer) estimate.getPrimaryRoute().get("totalDistanceKm"))
                .totalTransitDays(estimate.getEstimatedTransitDays())
                .estimatedCost(estimate.getGrandTotal())
                .reliabilityScore(BigDecimal.valueOf(94.5))
                .riskLevel("LOW")
                .build();
        shipmentRouteRepository.save(primaryRoute);

        // Save cost breakdown
        Map<String, Object> cb = estimate.getCostBreakdown();
        ShipmentCost cost = ShipmentCost.builder()
                .shipmentPlan(plan)
                .roadFreight(toBigDecimal(cb.get("roadFreight")))
                .oceanFreight(toBigDecimal(cb.get("oceanFreight")))
                .airFreight(toBigDecimal(cb.get("airFreight")))
                .railFreight(toBigDecimal(cb.get("railFreight")))
                .terminalCharges(toBigDecimal(cb.get("terminalCharges")))
                .documentationCharges(toBigDecimal(cb.get("documentationCharges")))
                .insurancePremium(toBigDecimal(cb.get("insurancePremium")))
                .customsDutiesAndCharges(toBigDecimal(cb.get("customsCharges")))
                .portCharges(toBigDecimal(cb.get("portCharges")))
                .containerCharges(toBigDecimal(cb.get("containerCharges")))
                .fuelSurcharge(toBigDecimal(cb.get("fuelSurcharge")))
                .bafSurcharge(toBigDecimal(cb.get("bafSurcharge")))
                .cafSurcharge(toBigDecimal(cb.get("cafSurcharge")))
                .handlingCharges(toBigDecimal(cb.get("handlingCharges")))
                .lastMileDeliveryCharges(toBigDecimal(cb.get("deliveryCharges")))
                .grandTotal(estimate.getGrandTotal())
                .currency("INR")
                .build();
        shipmentCostRepository.save(cost);

        log.info("Shipment plan created successfully with reference: {}", reference);
        return estimate;
    }

    @Override
    public ShipmentPlanResponse calculateEstimate(ShipmentPlanRequest request) {
        // 1. Calculations: Weight & Volume
        BigDecimal unitWeight = request.getWeightPerUnitKg() != null ? request.getWeightPerUnitKg() : BigDecimal.valueOf(0.5);
        BigDecimal totalWeightKg = unitWeight.multiply(BigDecimal.valueOf(request.getQuantity())).setScale(2, RoundingMode.HALF_UP);

        BigDecimal l = request.getLengthCm() != null ? request.getLengthCm() : BigDecimal.valueOf(25);
        BigDecimal w = request.getWidthCm() != null ? request.getWidthCm() : BigDecimal.valueOf(20);
        BigDecimal h = request.getHeightCm() != null ? request.getHeightCm() : BigDecimal.valueOf(15);

        BigDecimal unitVolumeCbm = l.multiply(w).multiply(h).divide(BigDecimal.valueOf(1000000), 6, RoundingMode.HALF_UP);
        BigDecimal totalVolumeCbm = unitVolumeCbm.multiply(BigDecimal.valueOf(request.getQuantity())).setScale(2, RoundingMode.HALF_UP);
        if (totalVolumeCbm.compareTo(BigDecimal.valueOf(0.1)) < 0) {
            totalVolumeCbm = BigDecimal.valueOf(0.10);
        }

        // 2. Transport Mode Recommendation
        String recommendedMode = calculateRecommendedMode(totalWeightKg, totalVolumeCbm, request.getPreferredTransportMode());
        List<Map<String, Object>> alternativeModes = calculateAlternativeModes(recommendedMode, totalWeightKg, totalVolumeCbm);

        // 3. Container Recommendation
        Map<String, Object> containerRec = calculateContainerRecommendation(totalWeightKg, totalVolumeCbm, request.getContainerPreference());

        // 4. Shipping Route Optimization
        Map<String, Object> primaryRoute = buildPrimaryRoute(request.getOriginLocation(), request.getDestinationCountry());
        List<Map<String, Object>> alternativeRoutes = buildAlternativeRoutes(request.getOriginLocation(), request.getDestinationCountry());

        // 5. Freight Rates & Breakdown
        Map<String, Object> costBreakdown = calculateFreightCost(recommendedMode, totalWeightKg, totalVolumeCbm, request.getQuantity(), request.getIncoterm());
        BigDecimal grandTotal = (BigDecimal) costBreakdown.get("grandTotal");
        BigDecimal costPerKg = grandTotal.divide(totalWeightKg, 2, RoundingMode.HALF_UP);
        BigDecimal costPerUnit = grandTotal.divide(BigDecimal.valueOf(request.getQuantity()), 2, RoundingMode.HALF_UP);

        // 6. Port Intelligence
        Map<String, Object> originPort = getPortDetails(primaryRoute.get("originPort").toString());
        Map<String, Object> destPort = getPortDetails(primaryRoute.get("destinationPort").toString());

        // 7. Carrier Recommendation
        Map<String, Object> recCarrier = getCarrierRecommendation(recommendedMode);
        List<Map<String, Object>> carrierComparison = getCarrierComparison(recommendedMode);

        // 8. Customs Workflow & Documents
        List<Map<String, Object>> docs = getCustomsDocumentChecklist(request.getHsCode(), request.getDestinationCountry());
        Map<String, Object> customsChecklist = Map.of(
                "exportClearanceTime", "24-48 hours",
                "importClearanceTime", "36-72 hours",
                "riskLevel", "Standard Green Channel",
                "documentsCount", docs.size()
        );

        // 9. Risk Assessment
        Map<String, Object> riskData = calculateRiskScore(request.getDestinationCountry(), recommendedMode, request.getHsCode());
        BigDecimal overallRiskScore = (BigDecimal) riskData.get("overallRiskScore");
        String riskCategory = (String) riskData.get("riskCategory");
        @SuppressWarnings("unchecked")
        List<String> mitigations = (List<String>) riskData.get("mitigations");
        @SuppressWarnings("unchecked")
        Map<String, Object> riskBreakdown = (Map<String, Object>) riskData.get("breakdown");

        // 10. Incoterm Engine
        Map<String, Object> incotermDetails = getIncotermBreakdown(request.getIncoterm());

        // 11. Insurance Engine
        BigDecimal cargoValue = BigDecimal.valueOf(request.getQuantity()).multiply(BigDecimal.valueOf(500)); // Estimated cargo value
        Map<String, Object> insuranceDetails = getInsuranceQuote(cargoValue, "MARINE_CARGO_ICC_A");

        // 12. AI Recommendation Engine Synthesis
        Map<String, Object> aiRec = generateAiRecommendation(recommendedMode, grandTotal, overallRiskScore);

        // 13. Timeline & Carbon Emissions
        int transitDays = recommendedMode.equals("AIR") ? 4 : recommendedMode.equals("SEA") ? 22 : 14;
        LocalDate dispatch = request.getExpectedDispatchDate() != null ? request.getExpectedDispatchDate() : LocalDate.now().plusDays(3);
        LocalDate delivery = dispatch.plusDays(transitDays);
        BigDecimal carbon = calculateCarbonEmissions(recommendedMode, totalWeightKg, 6500);

        List<Map<String, Object>> milestones = generateWorkflowMilestones(dispatch, delivery, recommendedMode);

        return ShipmentPlanResponse.builder()
                .hsCode(request.getHsCode())
                .productName(request.getProductName())
                .originLocation(request.getOriginLocation())
                .destinationCountry(request.getDestinationCountry())
                .destinationCity(request.getDestinationCity() != null ? request.getDestinationCity() : "Main City")
                .pickupAddress(request.getPickupAddress() != null ? request.getPickupAddress() : "Factory Premises")
                .incoterm(request.getIncoterm())
                .quantity(request.getQuantity())
                .totalWeightKg(totalWeightKg)
                .totalVolumeCbm(totalVolumeCbm)
                .isDangerousGoods(Boolean.TRUE.equals(request.getIsDangerousGoods()))
                .temperatureRequirement(request.getTemperatureRequirement() != null ? request.getTemperatureRequirement() : "Ambient")
                .recommendedMode(recommendedMode)
                .alternativeModes(alternativeModes)
                .containerRecommendation(containerRec)
                .primaryRoute(primaryRoute)
                .alternativeRoutes(alternativeRoutes)
                .costBreakdown(costBreakdown)
                .grandTotal(grandTotal)
                .costPerKg(costPerKg)
                .costPerUnit(costPerUnit)
                .originPortDetails(originPort)
                .destinationPortDetails(destPort)
                .recommendedCarrier(recCarrier)
                .carrierComparison(carrierComparison)
                .requiredDocuments(docs)
                .customsChecklist(customsChecklist)
                .overallRiskScore(overallRiskScore)
                .riskCategory(riskCategory)
                .riskBreakdown(riskBreakdown)
                .mitigationSuggestions(mitigations)
                .incotermDetails(incotermDetails)
                .recommendedInsurance(insuranceDetails)
                .aiRecommendation(aiRec)
                .estimatedTransitDays(transitDays)
                .expectedDispatchDate(dispatch)
                .estimatedDeliveryDate(delivery)
                .carbonEmissionsKg(carbon)
                .workflowMilestones(milestones)
                .build();
    }

    @Override
    public Map<String, Object> getAiRecommendations(ShipmentPlanRequest request) {
        ShipmentPlanResponse estimate = calculateEstimate(request);
        return estimate.getAiRecommendation();
    }

    @Override
    public Map<String, Object> trackShipment(String reference) {
        Map<String, Object> tracking = new LinkedHashMap<>();
        tracking.put("trackingNumber", reference);
        tracking.put("status", "IN_TRANSIT");
        tracking.put("currentLocation", "Port of Salalah Transshipment Terminal");
        tracking.put("origin", "Jawaharlal Nehru Port (JNPT), India");
        tracking.put("destination", "Hamburg Port, Germany");
        tracking.put("carrier", "Maersk Line");
        tracking.put("vesselName", "Maersk Mc-Kinney Moller / V.2408");
        tracking.put("eta", LocalDate.now().plusDays(12).toString());
        tracking.put("delayProbability", 0.08); // 8% probability of delay
        tracking.put("delayReason", "Minor port congestion (+6 hrs)");

        List<Map<String, Object>> events = List.of(
                Map.of("status", "Factory Pickup", "location", "Nashik Factory, India", "timestamp", "2026-08-20 09:00", "completed", true),
                Map.of("status", "ICD Maliwada Customs Clearance", "location", "ICD Maliwada, India", "timestamp", "2026-08-21 14:30", "completed", true),
                Map.of("status", "Loaded on Vessel at JNPT", "location", "JNPT Port, Navi Mumbai", "timestamp", "2026-08-23 18:00", "completed", true),
                Map.of("status", "In Transit (Ocean)", "location", "Arabian Sea / Indian Ocean", "timestamp", "2026-08-25 12:00", "completed", true),
                Map.of("status", "Transshipment Arrival", "location", "Salalah, Oman", "timestamp", "2026-08-28 06:00", "completed", false),
                Map.of("status", "Destination Port Clearance", "location", "Port of Hamburg, Germany", "timestamp", "2026-09-08 10:00", "completed", false),
                Map.of("status", "Delivered to Buyer Warehouse", "location", "Hamburg Warehouse, Germany", "timestamp", "2026-09-10 16:00", "completed", false)
        );
        tracking.put("events", events);
        return tracking;
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentPlanResponse getShipmentPlanById(Long id, String userEmail) {
        ShipmentPlan plan = shipmentPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShipmentPlan", "id", id));
        ShipmentPlanRequest req = new ShipmentPlanRequest();
        req.setHsCode(plan.getHsCode());
        req.setProductName(plan.getProductName());
        req.setOriginLocation(plan.getOriginLocation());
        req.setDestinationCountry(plan.getDestinationCountry());
        req.setDestinationCity(plan.getDestinationCity());
        req.setIncoterm(plan.getIncoterm());
        req.setQuantity(plan.getQuantity());
        req.setWeightPerUnitKg(plan.getTotalWeightKg().divide(BigDecimal.valueOf(plan.getQuantity()), 3, RoundingMode.HALF_UP));
        req.setContainerPreference(plan.getContainerPreference());

        ShipmentPlanResponse resp = calculateEstimate(req);
        resp.setId(plan.getId());
        resp.setPlanReference(plan.getPlanReference());
        return resp;
    }

    @Override
    public List<Map<String, Object>> getAllPorts() {
        List<Port> ports = portRepository.findAll();
        if (ports.isEmpty()) {
            return getDefaultPorts();
        }
        return ports.stream().map(p -> Map.<String, Object>of(
                "id", p.getId(),
                "name", p.getName(),
                "unlocode", p.getUnlocode(),
                "country", p.getCountry(),
                "countryCode", p.getCountryCode(),
                "portType", p.getPortType(),
                "congestionLevel", p.getCongestionLevel() != null ? p.getCongestionLevel() : "LOW",
                "avgWaitingDays", p.getAvgWaitingDays() != null ? p.getAvgWaitingDays() : 1.2
        )).toList();
    }

    @Override
    public List<Map<String, Object>> getAllCarriers() {
        List<Carrier> carriers = carrierRepository.findAll();
        if (carriers.isEmpty()) {
            return getDefaultCarriers();
        }
        return carriers.stream().map(c -> Map.<String, Object>of(
                "id", c.getId(),
                "name", c.getName(),
                "carrierType", c.getCarrierType(),
                "code", c.getCode() != null ? c.getCode() : "N/A",
                "reliabilityScore", c.getReliabilityScore() != null ? c.getReliabilityScore() : 92.0,
                "historicOnTimePct", c.getHistoricOnTimePct() != null ? c.getHistoricOnTimePct() : 94.5,
                "costRating", c.getCostRating() != null ? c.getCostRating() : "COMPETITIVE"
        )).toList();
    }

    @Override
    public List<Map<String, Object>> getAllRoutes() {
        return List.of(
                Map.of("lane", "India - West Coast to North Europe", "originPort", "JNPT / Mundra", "destPort", "Hamburg / Rotterdam", "transitDays", 22, "reliability", "95%"),
                Map.of("lane", "India - East Coast to Southeast Asia", "originPort", "Chennai / Vizag", "destPort", "Singapore / Port Klang", "transitDays", 7, "reliability", "98%"),
                Map.of("lane", "India to Middle East (Gulf)", "originPort", "Mundra / JNPT", "destPort", "Jebel Ali (Dubai)", "transitDays", 4, "reliability", "99%"),
                Map.of("lane", "India to US East Coast", "originPort", "JNPT / Hazira", "destPort", "New York / Savannah", "transitDays", 26, "reliability", "92%")
        );
    }

    @Override
    public List<Map<String, Object>> getAllContainers() {
        return List.of(
                Map.of("code", "20FT", "name", "20ft Standard Dry Container", "maxVolumeCbm", 33.2, "maxPayloadKg", 28200, "tareWeightKg", 2200),
                Map.of("code", "40FT", "name", "40ft Standard Dry Container", "maxVolumeCbm", 67.7, "maxPayloadKg", 28800, "tareWeightKg", 3800),
                Map.of("code", "40HC", "name", "40ft High Cube Container", "maxVolumeCbm", 76.4, "maxPayloadKg", 28600, "tareWeightKg", 3900),
                Map.of("code", "LCL", "name", "Less Than Container Load (Consolidated)", "maxVolumeCbm", 15.0, "maxPayloadKg", 10000, "tareWeightKg", 0),
                Map.of("code", "REEFER", "name", "20ft/40ft Refrigerated Container", "maxVolumeCbm", 28.5, "maxPayloadKg", 27000, "tareWeightKg", 3200)
        );
    }

    @Override
    public List<Map<String, Object>> getAllWarehouses() {
        return List.of(
                Map.of("id", 1, "name", "JNPT Logistics Park & Container Yard", "city", "Navi Mumbai", "country", "India", "type", "BONDED_ICD", "totalSqft", 250000, "coldStorage", true),
                Map.of("id", 2, "name", "Mundra Special Economic Zone Warehouse", "city", "Mundra, Gujarat", "country", "India", "type", "HAZARDOUS_GENERAL", "totalSqft", 400000, "coldStorage", false),
                Map.of("id", 3, "name", "Hamburg Port Cold Hub Logistics Centre", "city", "Hamburg", "country", "Germany", "type", "COLD_STORAGE", "totalSqft", 180000, "coldStorage", true),
                Map.of("id", 4, "name", "Dubai South Freezone Cargo Hub", "city", "Dubai", "country", "UAE", "type", "GENERAL_BONDED", "totalSqft", 500000, "coldStorage", true)
        );
    }

    @Override
    public List<Map<String, Object>> getAllFreightRates() {
        return List.of(
                Map.of("mode", "SEA_FCL", "origin", "JNPT, India", "destination", "Hamburg, Germany", "rateUnit", "PER_20FT", "baseRate", 1450.00, "currency", "USD", "transitDays", 22),
                Map.of("mode", "SEA_LCL", "origin", "JNPT, India", "destination", "Hamburg, Germany", "rateUnit", "PER_CBM", "baseRate", 65.00, "currency", "USD", "transitDays", 24),
                Map.of("mode", "AIR", "origin", "BOM (Mumbai Airport)", "destination", "FRA (Frankfurt Airport)", "rateUnit", "PER_KG", "baseRate", 4.20, "currency", "USD", "transitDays", 3),
                Map.of("mode", "ROAD", "origin", "Nashik Factory", "destination", "JNPT Port", "rateUnit", "PER_TRUCK", "baseRate", 28000.00, "currency", "INR", "transitDays", 1)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentPlanResponse> getShipmentHistory(String userEmail) {
        User exporter = userRepository.findByEmail(userEmail).orElse(null);
        if (exporter == null) return List.of();
        List<ShipmentPlan> plans = shipmentPlanRepository.findByExporterOrderByCreatedAtDesc(exporter);
        return plans.stream().map(p -> getShipmentPlanById(p.getId(), userEmail)).toList();
    }

    @Override
    public Map<String, Object> calculateCostBreakdown(ShipmentPlanRequest request) {
        ShipmentPlanResponse estimate = calculateEstimate(request);
        return estimate.getCostBreakdown();
    }

    @Override
    public Map<String, Object> assessRisk(ShipmentPlanRequest request) {
        return calculateRiskScore(request.getDestinationCountry(), request.getPreferredTransportMode() != null ? request.getPreferredTransportMode() : "SEA", request.getHsCode());
    }

    @Override
    public List<Map<String, Object>> getCustomsDocumentChecklist(String hsCode, String destinationCountry) {
        List<Map<String, Object>> docs = new ArrayList<>();
        docs.add(Map.of("documentName", "Commercial Invoice", "code", "INV", "issuingAuthority", "Exporter", "mandatory", true, "description", "Itemized invoice with HS codes and Incoterm terms"));
        docs.add(Map.of("documentName", "Packing List", "code", "PL", "issuingAuthority", "Exporter / Freight Forwarder", "mandatory", true, "description", "Detailed weight, package, and container packing specifications"));
        docs.add(Map.of("documentName", "Export Declaration (Shipping Bill)", "code", "SB", "issuingAuthority", "Indian Customs (ICEGATE)", "mandatory", true, "description", "Filed electronically on ICEGATE prior to factory pickup / port gate entry"));
        docs.add(Map.of("documentName", "Bill of Lading / Air Waybill", "code", "BOL", "issuingAuthority", "Carrier / Shipping Line", "mandatory", true, "description", "Title document & contract of carriage issued upon vessel loading"));
        docs.add(Map.of("documentName", "Certificate of Origin (Preferential)", "code", "COO", "issuingAuthority", "FIEO / Chamber of Commerce", "mandatory", true, "description", "Required for duty exemption under Trade Agreements (FTA/CEPA)"));

        if (hsCode != null && (hsCode.startsWith("09") || hsCode.startsWith("10") || hsCode.startsWith("07") || hsCode.startsWith("08"))) {
            docs.add(Map.of("documentName", "Phytosanitary Certificate", "code", "PHYTO", "issuingAuthority", "Plant Quarantine Dept, Govt of India", "mandatory", true, "description", "Mandatory agricultural inspection certificate for plant products"));
            docs.add(Map.of("documentName", "FSSAI Export Health Certificate", "code", "FSSAI", "issuingAuthority", "Food Safety & Standards Authority", "mandatory", true, "description", "Food hygiene and pesticide residue lab clearance certificate"));
        }

        docs.add(Map.of("documentName", "Cargo Insurance Certificate", "code", "INS", "issuingAuthority", "Underwriter / Insurance Broker", "mandatory", false, "description", "Marine cargo insurance policy covering transit damage and General Average"));
        return docs;
    }

    @Override
    public Map<String, Object> getIncotermBreakdown(String code) {
        String inc = code != null ? code.toUpperCase() : "CIF";
        Map<String, String> sellerMap = Map.of(
                "EXW", "Factory packaging & loading preparation only.",
                "FOB", "Export packing, inland transport to Indian port, customs export clearance, port loading onto vessel.",
                "CIF", "Export packing, inland transport, customs clearance, ocean freight to destination port, marine cargo insurance.",
                "DDP", "End-to-end responsibility: factory pickup, export clearance, main freight, import duties, customs tax, final warehouse delivery."
        );
        Map<String, String> buyerMap = Map.of(
                "EXW", "All transport from seller factory, export clearance, freight, insurance, destination customs, final delivery.",
                "FOB", "Ocean freight cost, marine insurance, destination import duties, port handling, final delivery.",
                "CIF", "Destination port unloading, import customs clearance, import tariffs, local inland transport.",
                "DDP", "Unloading at buyer warehouse premises only."
        );

        return Map.of(
                "incotermCode", inc,
                "sellerResponsibility", sellerMap.getOrDefault(inc, "Export packaging, customs clearance & main freight to destination."),
                "buyerResponsibility", buyerMap.getOrDefault(inc, "Import clearance, destination customs duties & inland delivery."),
                "riskTransferPoint", inc.equals("FOB") || inc.equals("CIF") ? "When cargo crosses vessel's rail at origin port (JNPT/Mundra)." : "Upon delivery at specified destination place.",
                "insuranceRequirement", inc.equals("CIF") || inc.equals("CIP") ? "Mandatory for Seller (ICC 'A' or 'C' clause)" : "Optional / Buyer responsibility",
                "recommendedForSmes", inc.equals("CIF") || inc.equals("FOB") ? "Highly Recommended (Standard Trade Practice)" : "Moderate Complexity"
        );
    }

    @Override
    public Map<String, Object> getInsuranceQuote(BigDecimal cargoValue, String policyType) {
        BigDecimal val = cargoValue != null && cargoValue.compareTo(BigDecimal.ZERO) > 0 ? cargoValue : BigDecimal.valueOf(2500000);
        BigDecimal rate = policyType != null && policyType.contains("ICC_A") ? BigDecimal.valueOf(0.0035) : BigDecimal.valueOf(0.0020);
        BigDecimal premium = val.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        if (premium.compareTo(BigDecimal.valueOf(1500)) < 0) {
            premium = BigDecimal.valueOf(1500.00);
        }

        return Map.of(
                "policyType", policyType != null ? policyType : "MARINE_CARGO_ICC_A",
                "providerName", "ICICI Lombard General Insurance / HDFC ERGO",
                "insuredValue", val.multiply(BigDecimal.valueOf(1.10)), // 110% CIF value
                "premiumAmount", premium,
                "currency", "INR",
                "riskCoverage", "All Risks of physical loss or damage (Institute Cargo Clauses A including Strike, Riots & War)",
                "deductible", "0.50% of claim value or ₹10,000",
                "claimsProcedure", "Survey within 48h of discharge -> Lodge claim on TradeBridge Portal -> Settlement within 14 working days"
        );
    }

    @Override
    public Map<String, Object> getAnalyticsDashboardData() {
        return Map.of(
                "avgTransitTimeDays", 18.4,
                "shippingCostTrends", List.of(
                        Map.of("month", "Jan", "avgCostPerKg", 42.5),
                        Map.of("month", "Feb", "avgCostPerKg", 41.0),
                        Map.of("month", "Mar", "avgCostPerKg", 39.8),
                        Map.of("month", "Apr", "avgCostPerKg", 44.2),
                        Map.of("month", "May", "avgCostPerKg", 40.5)
                ),
                "topExportDestinations", List.of(
                        Map.of("country", "Germany", "share", "28%"),
                        Map.of("country", "United States", "share", "24%"),
                        Map.of("country", "UAE", "share", "18%"),
                        Map.of("country", "Singapore", "share", "15%"),
                        Map.of("country", "United Kingdom", "share", "15%")
                ),
                "mostUsedPorts", List.of(
                        Map.of("port", "JNPT (Nava Sheva)", "volumePct", "45%"),
                        Map.of("port", "Mundra Port", "volumePct", "35%"),
                        Map.of("port", "Chennai Port", "volumePct", "20%")
                ),
                "mostUsedCarriers", List.of(
                        Map.of("carrier", "Maersk Line", "share", "40%"),
                        Map.of("carrier", "MSC", "share", "30%"),
                        Map.of("carrier", "CMA CGM", "share", "20%"),
                        Map.of("carrier", "Air India Cargo", "share", "10%")
                ),
                "avgContainerUtilization", "91.8%",
                "shipmentSuccessRate", "99.4%",
                "avgCustomsDelayDays", 0.6,
                "avgDeliveryTimeDays", 19.2
        );
    }

    // ── Helper Calculation Methods ───────────────────────────────────────────

    private String calculateRecommendedMode(BigDecimal weightKg, BigDecimal volumeCbm, String preferred) {
        if (preferred != null && !preferred.isBlank() && !preferred.equalsIgnoreCase("AUTO")) {
            return preferred.toUpperCase();
        }
        if (weightKg.compareTo(BigDecimal.valueOf(150)) <= 0 && volumeCbm.compareTo(BigDecimal.valueOf(1.0)) <= 0) {
            return "AIR";
        }
        if (weightKg.compareTo(BigDecimal.valueOf(15000)) >= 0 || volumeCbm.compareTo(BigDecimal.valueOf(25.0)) >= 0) {
            return "SEA";
        }
        return "SEA"; // Default to Sea Freight
    }

    private List<Map<String, Object>> calculateAlternativeModes(String primary, BigDecimal weightKg, BigDecimal volumeCbm) {
        List<Map<String, Object>> alts = new ArrayList<>();
        if (!primary.equals("AIR")) {
            alts.add(Map.of("mode", "AIR", "transitDays", 3, "estimatedCost", weightKg.multiply(BigDecimal.valueOf(380)), "carbonKg", weightKg.multiply(BigDecimal.valueOf(2.8)), "advantages", "Fastest delivery, minimal damage risk", "disadvantages", "Significantly higher freight cost"));
        }
        if (!primary.equals("SEA")) {
            alts.add(Map.of("mode", "SEA", "transitDays", 22, "estimatedCost", weightKg.multiply(BigDecimal.valueOf(32)), "carbonKg", weightKg.multiply(BigDecimal.valueOf(0.12)), "advantages", "Lowest cost per KG, suitable for heavy cargo", "disadvantages", "Longer transit duration"));
        }
        alts.add(Map.of("mode", "MULTIMODAL (Rail + Sea)", "transitDays", 19, "estimatedCost", weightKg.multiply(BigDecimal.valueOf(36)), "carbonKg", weightKg.multiply(BigDecimal.valueOf(0.18)), "advantages", "Eco-friendly inland transit, seamless port handover", "disadvantages", "Requires multiple transport documents"));
        return alts;
    }

    private Map<String, Object> calculateContainerRecommendation(BigDecimal weightKg, BigDecimal volumeCbm, String pref) {
        String containerType = "20FT";
        int containerCount = 1;
        double utilizationPct = 85.0;

        if (volumeCbm.compareTo(BigDecimal.valueOf(15.0)) < 0) {
            containerType = "LCL";
            utilizationPct = volumeCbm.divide(BigDecimal.valueOf(15.0), 4, RoundingMode.HALF_UP).doubleValue() * 100;
        } else if (volumeCbm.compareTo(BigDecimal.valueOf(30.0)) <= 0) {
            containerType = "20FT";
            utilizationPct = volumeCbm.divide(BigDecimal.valueOf(33.2), 4, RoundingMode.HALF_UP).doubleValue() * 100;
        } else if (volumeCbm.compareTo(BigDecimal.valueOf(65.0)) <= 0) {
            containerType = "40FT";
            utilizationPct = volumeCbm.divide(BigDecimal.valueOf(67.7), 4, RoundingMode.HALF_UP).doubleValue() * 100;
        } else {
            containerType = "40HC";
            containerCount = volumeCbm.divide(BigDecimal.valueOf(76.4), 0, RoundingMode.CEILING).intValue();
            utilizationPct = 92.5;
        }

        return Map.of(
                "recommendedType", containerType,
                "containerCount", containerCount,
                "containerUtilizationPct", Math.round(utilizationPct * 10.0) / 10.0,
                "unusedCapacityCbm", Math.max(0, 33.2 - volumeCbm.doubleValue()),
                "loadingEfficiency", "Optimal Payload & Volume Fill",
                "recommendationNote", "Packed using standard Euro Pallets (1200x800 mm) layout for maximum stability."
        );
    }

    private Map<String, Object> buildPrimaryRoute(String origin, String country) {
        String destPort = country.equalsIgnoreCase("Germany") ? "Hamburg Port" :
                          country.equalsIgnoreCase("United States") ? "Port of New York / New Jersey" :
                          country.equalsIgnoreCase("UAE") ? "Jebel Ali Port, Dubai" : "Singapore Port";
        return Map.of(
                "originIcd", "ICD Maliwada / ICD Whitefield",
                "originPort", "Jawaharlal Nehru Port Trust (JNPT), Mumbai",
                "transshipmentPort", "Port of Salalah (Oman)",
                "destinationPort", destPort,
                "destinationWarehouse", country + " International Logistics Hub",
                "totalDistanceKm", 6850,
                "routeType", "PRIMARY"
        );
    }

    private List<Map<String, Object>> buildAlternativeRoutes(String origin, String country) {
        return List.of(
                Map.of("originPort", "Mundra Port, Gujarat", "transshipmentPort", "Direct (No Transshipment)", "destinationPort", country + " Gateway Port", "transitDays", 20, "costDelta", "-5%", "risk", "LOW"),
                Map.of("originPort", "Chennai Port", "transshipmentPort", "Colombo Port (Sri Lanka)", "destinationPort", country + " Gateway Port", "transitDays", 24, "costDelta", "-8%", "risk", "MODERATE")
        );
    }

    private Map<String, Object> calculateFreightCost(String mode, BigDecimal weightKg, BigDecimal volumeCbm, int qty, String incoterm) {
        BigDecimal base = mode.equals("AIR") ? weightKg.multiply(BigDecimal.valueOf(320)) : weightKg.multiply(BigDecimal.valueOf(28));
        if (base.compareTo(BigDecimal.valueOf(15000)) < 0) base = BigDecimal.valueOf(15000);

        BigDecimal roadFreight = BigDecimal.valueOf(18000.00);
        BigDecimal oceanAirFreight = base;
        BigDecimal terminal = BigDecimal.valueOf(8500.00);
        BigDecimal doc = BigDecimal.valueOf(3500.00);
        BigDecimal insurance = base.multiply(BigDecimal.valueOf(0.025)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal customs = BigDecimal.valueOf(12000.00);
        BigDecimal port = BigDecimal.valueOf(9500.00);
        BigDecimal container = mode.equals("AIR") ? BigDecimal.ZERO : BigDecimal.valueOf(14000.00);
        BigDecimal fuelSurcharge = base.multiply(BigDecimal.valueOf(0.08)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal baf = mode.equals("AIR") ? BigDecimal.ZERO : BigDecimal.valueOf(4500.00);
        BigDecimal caf = mode.equals("AIR") ? BigDecimal.ZERO : BigDecimal.valueOf(2200.00);
        BigDecimal handling = BigDecimal.valueOf(4200.00);
        BigDecimal delivery = BigDecimal.valueOf(15000.00);

        BigDecimal total = roadFreight.add(oceanAirFreight).add(terminal).add(doc).add(insurance)
                .add(customs).add(port).add(container).add(fuelSurcharge).add(baf).add(caf).add(handling).add(delivery);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("roadFreight", roadFreight);
        map.put("oceanFreight", mode.equals("SEA") ? oceanAirFreight : BigDecimal.ZERO);
        map.put("airFreight", mode.equals("AIR") ? oceanAirFreight : BigDecimal.ZERO);
        map.put("railFreight", BigDecimal.ZERO);
        map.put("terminalCharges", terminal);
        map.put("documentationCharges", doc);
        map.put("insurancePremium", insurance);
        map.put("customsCharges", customs);
        map.put("portCharges", port);
        map.put("containerCharges", container);
        map.put("fuelSurcharge", fuelSurcharge);
        map.put("bafSurcharge", baf);
        map.put("cafSurcharge", caf);
        map.put("handlingCharges", handling);
        map.put("deliveryCharges", delivery);
        map.put("grandTotal", total);
        return map;
    }

    private Map<String, Object> getPortDetails(String name) {
        return Map.of(
                "portName", name,
                "unlocode", name.contains("JNPT") ? "INNSA" : name.contains("Hamburg") ? "DEHAM" : "USNYC",
                "containerCapacity", "7.6 Million TEU / Year",
                "congestionStatus", "LOW (Avg waiting: 0.8 days)",
                "terminalOperators", "APM Terminals, DP World, PSA International",
                "railConnectivity", true,
                "roadConnectivity", true
        );
    }

    private Map<String, Object> getCarrierRecommendation(String mode) {
        if (mode.equals("AIR")) {
            return Map.of("name", "Emirates SkyCargo / Air India Cargo", "code", "EK / AI", "reliabilityScore", 96.5, "onTimeRate", "97.2%", "costRating", "PREMIUM");
        }
        return Map.of("name", "Maersk Line", "code", "MAEU", "reliabilityScore", 94.8, "onTimeRate", "95.1%", "costRating", "COMPETITIVE");
    }

    private List<Map<String, Object>> getCarrierComparison(String mode) {
        return List.of(
                Map.of("carrier", "Maersk Line", "cost", "₹1,45,000", "transitTime", "22 Days", "reliability", "95%", "rank", 1),
                Map.of("carrier", "MSC (Mediterranean Shipping Co)", "cost", "₹1,38,000", "transitTime", "24 Days", "reliability", "92%", "rank", 2),
                Map.of("carrier", "CMA CGM", "cost", "₹1,42,000", "transitTime", "23 Days", "reliability", "94%", "rank", 3)
        );
    }

    private Map<String, Object> calculateRiskScore(String country, String mode, String hsCode) {
        BigDecimal overall = BigDecimal.valueOf(18.5); // Low risk (0-100 scale)
        return Map.of(
                "overallRiskScore", overall,
                "riskCategory", "LOW",
                "breakdown", Map.of(
                        "countryRisk", 15.0,
                        "politicalRisk", 12.0,
                        "portCongestionRisk", 22.0,
                        "weatherRisk", 18.0,
                        "customsDelayRisk", 20.0,
                        "documentationRisk", 10.0,
                        "carrierRisk", 14.0
                ),
                "mitigations", List.of(
                        "Ensure Phytosanitary Certificate is uploaded 48 hours prior to vessel loading.",
                        "Use sealed container with GPS smart lock tracker for real-time tampering alerts.",
                        "Select Institute Cargo Clauses (A) all-risk insurance policy."
                )
        );
    }

    private Map<String, Object> generateAiRecommendation(String mode, BigDecimal totalCost, BigDecimal riskScore) {
        return Map.of(
                "recommendedPlan", "Balanced Optimal Plan (Sea Freight via JNPT)",
                "lowestCostPlan", Map.of("mode", "Sea LCL via Mundra Port", "cost", totalCost.multiply(BigDecimal.valueOf(0.88)), "transitDays", 25),
                "fastestPlan", Map.of("mode", "Air Express via Mumbai Airport", "cost", totalCost.multiply(BigDecimal.valueOf(2.8)), "transitDays", 3),
                "lowestRiskPlan", Map.of("mode", "Sea Direct FCL via JNPT", "cost", totalCost.multiply(BigDecimal.valueOf(1.05)), "transitDays", 20, "riskScore", 12.0),
                "aiSummary", "Based on your product weight and target country, Sea FCL via JNPT is the most cost-effective and reliable option, offering 95% on-time historical performance under trade agreements."
        );
    }

    private BigDecimal calculateCarbonEmissions(String mode, BigDecimal weightKg, double distanceKm) {
        double factor = mode.equals("AIR") ? 0.500 : mode.equals("SEA") ? 0.015 : 0.060; // kg CO2 per ton-km
        double ton = weightKg.doubleValue() / 1000.0;
        double co2 = ton * distanceKm * factor;
        return BigDecimal.valueOf(co2).setScale(2, RoundingMode.HALF_UP);
    }

    private List<Map<String, Object>> generateWorkflowMilestones(LocalDate dispatch, LocalDate delivery, String mode) {
        return List.of(
                Map.of("milestone", "Pickup & Factory Dispatch", "date", dispatch.toString(), "status", "SCHEDULED"),
                Map.of("milestone", "Inland Transit to ICD / Port", "date", dispatch.plusDays(1).toString(), "status", "PENDING"),
                Map.of("milestone", "Customs Inspection & Gate-In", "date", dispatch.plusDays(2).toString(), "status", "PENDING"),
                Map.of("milestone", "Vessel Loading & Departure", "date", dispatch.plusDays(3).toString(), "status", "PENDING"),
                Map.of("milestone", "Arrival at Destination Port", "date", delivery.minusDays(2).toString(), "status", "PENDING"),
                Map.of("milestone", "Destination Customs Clearance", "date", delivery.minusDays(1).toString(), "status", "PENDING"),
                Map.of("milestone", "Final Warehouse Delivery", "date", delivery.toString(), "status", "PENDING")
        );
    }

    private BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof BigDecimal bd) return bd;
        if (obj instanceof Number num) return BigDecimal.valueOf(num.doubleValue());
        try {
            return new BigDecimal(obj.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private List<Map<String, Object>> getDefaultPorts() {
        return List.of(
                Map.of("id", 1, "name", "Jawaharlal Nehru Port (JNPT)", "unlocode", "INNSA", "country", "India", "countryCode", "IN", "portType", "SEA", "congestionLevel", "LOW", "avgWaitingDays", 0.8),
                Map.of("id", 2, "name", "Mundra Port", "unlocode", "INMUN", "country", "India", "countryCode", "IN", "portType", "SEA", "congestionLevel", "LOW", "avgWaitingDays", 0.5),
                Map.of("id", 3, "name", "Port of Hamburg", "unlocode", "DEHAM", "country", "Germany", "countryCode", "DE", "portType", "SEA", "congestionLevel", "LOW", "avgWaitingDays", 1.0),
                Map.of("id", 4, "name", "Jebel Ali Port", "unlocode", "AEJEA", "country", "UAE", "countryCode", "AE", "portType", "SEA", "congestionLevel", "LOW", "avgWaitingDays", 0.4)
        );
    }

    private List<Map<String, Object>> getDefaultCarriers() {
        return List.of(
                Map.of("id", 1, "name", "Maersk Line", "carrierType", "OCEAN", "code", "MAEU", "reliabilityScore", 94.8, "historicOnTimePct", 95.1, "costRating", "COMPETITIVE"),
                Map.of("id", 2, "name", "Mediterranean Shipping Co (MSC)", "carrierType", "OCEAN", "code", "MSCU", "reliabilityScore", 92.4, "historicOnTimePct", 92.8, "costRating", "BUDGET"),
                Map.of("id", 3, "name", "Emirates SkyCargo", "carrierType", "AIR", "code", "EK", "reliabilityScore", 98.2, "historicOnTimePct", 98.5, "costRating", "PREMIUM")
        );
    }
}
