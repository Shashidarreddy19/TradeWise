package com.trade.service;

import com.trade.dto.shipment.ShipmentPlanRequest;
import com.trade.dto.shipment.ShipmentPlanResponse;
import com.trade.repository.*;
import com.trade.service.impl.LogisticsPlannerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class LogisticsPlannerServiceTest {

    @Mock private ShipmentPlanRepository shipmentPlanRepository;
    @Mock private ShipmentRouteRepository shipmentRouteRepository;
    @Mock private ShipmentCostRepository shipmentCostRepository;
    @Mock private PortRepository portRepository;
    @Mock private ContainerTypeRepository containerTypeRepository;
    @Mock private CarrierRepository carrierRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private IncotermRepository incotermRepository;
    @Mock private FreightRateRepository freightRateRepository;
    @Mock private InsuranceOptionRepository insuranceOptionRepository;
    @Mock private UserRepository userRepository;

    private LogisticsPlannerService logisticsPlannerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        logisticsPlannerService = new LogisticsPlannerServiceImpl(
                shipmentPlanRepository,
                shipmentRouteRepository,
                shipmentCostRepository,
                portRepository,
                containerTypeRepository,
                carrierRepository,
                warehouseRepository,
                incotermRepository,
                freightRateRepository,
                insuranceOptionRepository,
                userRepository
        );

        when(portRepository.findAll()).thenReturn(Collections.emptyList());
        when(carrierRepository.findAll()).thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("Test Estimate Calculation for Sea Freight (FCL 20FT)")
    void testCalculateEstimateSeaFreight() {
        ShipmentPlanRequest request = new ShipmentPlanRequest();
        request.setHsCode("0910.30");
        request.setProductName("Turmeric Powder");
        request.setOriginLocation("Nashik, Maharashtra");
        request.setDestinationCountry("Germany");
        request.setIncoterm("CIF");
        request.setQuantity(5000);
        request.setWeightPerUnitKg(BigDecimal.valueOf(1.0));
        request.setLengthCm(BigDecimal.valueOf(30));
        request.setWidthCm(BigDecimal.valueOf(20));
        request.setHeightCm(BigDecimal.valueOf(20));
        request.setPreferredTransportMode("SEA");

        ShipmentPlanResponse response = logisticsPlannerService.calculateEstimate(request);

        assertNotNull(response);
        assertEquals("0910.30", response.getHsCode());
        assertEquals("Turmeric Powder", response.getProductName());
        assertEquals("SEA", response.getRecommendedMode());
        assertEquals(BigDecimal.valueOf(5000.00).setScale(2), response.getTotalWeightKg());
        assertNotNull(response.getGrandTotal());
        assertTrue(response.getGrandTotal().compareTo(BigDecimal.ZERO) > 0);
        assertNotNull(response.getRequiredDocuments());
        assertTrue(response.getRequiredDocuments().size() >= 5);
        assertNotNull(response.getOverallRiskScore());
        assertEquals("CIF", response.getIncoterm());
    }

    @Test
    @DisplayName("Test Container Optimization Logic for Small Package (LCL)")
    void testContainerRecommendationLCL() {
        ShipmentPlanRequest request = new ShipmentPlanRequest();
        request.setHsCode("0910.30");
        request.setProductName("Spice Extracts");
        request.setOriginLocation("Kochi, Kerala");
        request.setDestinationCountry("United States");
        request.setIncoterm("FOB");
        request.setQuantity(100);
        request.setWeightPerUnitKg(BigDecimal.valueOf(0.5));
        request.setLengthCm(BigDecimal.valueOf(10));
        request.setWidthCm(BigDecimal.valueOf(10));
        request.setHeightCm(BigDecimal.valueOf(10));
        request.setPreferredTransportMode("SEA");

        ShipmentPlanResponse response = logisticsPlannerService.calculateEstimate(request);

        assertNotNull(response.getContainerRecommendation());
        Map<String, Object> container = response.getContainerRecommendation();
        assertEquals("LCL", container.get("recommendedType"));
    }

    @Test
    @DisplayName("Test Customs Document Generator for Agricultural HS Code")
    void testCustomsDocumentGenerator() {
        var docs = logisticsPlannerService.getCustomsDocumentChecklist("0910.30", "Germany");
        assertNotNull(docs);
        assertTrue(docs.stream().anyMatch(d -> d.get("code").equals("PHYTO")));
        assertTrue(docs.stream().anyMatch(d -> d.get("code").equals("FSSAI")));
    }
}