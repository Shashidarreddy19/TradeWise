package com.trade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipment_routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_plan_id", nullable = false)
    private ShipmentPlan shipmentPlan;

    @Column(name = "route_type", length = 20)
    private String routeType; // PRIMARY, ALTERNATIVE_1, ALTERNATIVE_2

    @Column(name = "origin_icd", length = 100)
    private String originIcd;

    @Column(name = "origin_port", nullable = false, length = 100)
    private String originPort;

    @Column(name = "transshipment_port", length = 100)
    private String transshipmentPort;

    @Column(name = "destination_port", nullable = false, length = 100)
    private String destinationPort;

    @Column(name = "destination_warehouse", length = 100)
    private String destinationWarehouse;

    @Column(name = "total_distance_km")
    private Integer totalDistanceKm;

    @Column(name = "total_transit_days")
    private Integer totalTransitDays;

    @Column(name = "estimated_cost", precision = 12, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "reliability_score", precision = 5, scale = 2)
    private BigDecimal reliabilityScore;

    @Column(name = "risk_level", length = 20)
    private String riskLevel; // LOW, MEDIUM, HIGH

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
