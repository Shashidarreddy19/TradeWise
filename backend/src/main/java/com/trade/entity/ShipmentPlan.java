package com.trade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipment_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_reference", nullable = false, unique = true, length = 50)
    private String planReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exporter_id")
    private User exporter;

    @Column(name = "hs_code", nullable = false, length = 20)
    private String hsCode;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "origin_location", nullable = false, length = 255)
    private String originLocation;

    @Column(name = "destination_country", nullable = false, length = 100)
    private String destinationCountry;

    @Column(name = "destination_city", length = 100)
    private String destinationCity;

    @Column(name = "pickup_address", columnDefinition = "TEXT")
    private String pickupAddress;

    @Column(nullable = false, length = 20)
    private String incoterm; // EXW, FOB, CIF, DDP, etc.

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "total_weight_kg", nullable = false, precision = 12, scale = 3)
    private BigDecimal totalWeightKg;

    @Column(name = "total_volume_cbm", nullable = false, precision = 12, scale = 3)
    private BigDecimal totalVolumeCbm;

    @Column(name = "recommended_mode", length = 50)
    private String recommendedMode; // SEA, AIR, ROAD, RAIL, MULTIMODAL

    @Column(name = "container_preference", length = 50)
    private String containerPreference; // 20FT, 40FT, LCL, REEFER, etc.

    @Column(name = "is_dangerous_goods")
    @Builder.Default
    private Boolean isDangerousGoods = false;

    @Column(name = "temperature_requirement", length = 50)
    private String temperatureRequirement;

    @Column(name = "expected_dispatch_date")
    private LocalDate expectedDispatchDate;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "estimated_transit_days")
    private Integer estimatedTransitDays;

    @Column(name = "total_estimated_cost", precision = 14, scale = 2)
    private BigDecimal totalEstimatedCost;

    @Column(name = "cost_per_kg", precision = 10, scale = 2)
    private BigDecimal costPerKg;

    @Column(name = "cost_per_unit", precision = 10, scale = 2)
    private BigDecimal costPerUnit;

    @Column(name = "overall_risk_score", precision = 5, scale = 2)
    private BigDecimal overallRiskScore;

    @Column(name = "carbon_emissions_kg", precision = 10, scale = 2)
    private BigDecimal carbonEmissionsKg;

    @Column(name = "recommended_carrier", length = 100)
    private String recommendedCarrier;

    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "PLANNED"; // PLANNED, BOOKED, IN_TRANSIT, DELIVERED, CANCELLED

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
