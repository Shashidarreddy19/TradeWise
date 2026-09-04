package com.trade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipment_costs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentCost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_plan_id", nullable = false)
    private ShipmentPlan shipmentPlan;

    @Column(name = "road_freight", precision = 12, scale = 2)
    private BigDecimal roadFreight;

    @Column(name = "ocean_freight", precision = 12, scale = 2)
    private BigDecimal oceanFreight;

    @Column(name = "air_freight", precision = 12, scale = 2)
    private BigDecimal airFreight;

    @Column(name = "rail_freight", precision = 12, scale = 2)
    private BigDecimal railFreight;

    @Column(name = "terminal_charges", precision = 10, scale = 2)
    private BigDecimal terminalCharges;

    @Column(name = "documentation_charges", precision = 10, scale = 2)
    private BigDecimal documentationCharges;

    @Column(name = "insurance_premium", precision = 10, scale = 2)
    private BigDecimal insurancePremium;

    @Column(name = "customs_duties_and_charges", precision = 10, scale = 2)
    private BigDecimal customsDutiesAndCharges;

    @Column(name = "port_charges", precision = 10, scale = 2)
    private BigDecimal portCharges;

    @Column(name = "container_charges", precision = 10, scale = 2)
    private BigDecimal containerCharges;

    @Column(name = "fuel_surcharge", precision = 10, scale = 2)
    private BigDecimal fuelSurcharge;

    @Column(name = "baf_surcharge", precision = 10, scale = 2)
    private BigDecimal bafSurcharge;

    @Column(name = "caf_surcharge", precision = 10, scale = 2)
    private BigDecimal cafSurcharge;

    @Column(name = "handling_charges", precision = 10, scale = 2)
    private BigDecimal handlingCharges;

    @Column(name = "last_mile_delivery_charges", precision = 10, scale = 2)
    private BigDecimal lastMileDeliveryCharges;

    @Column(name = "grand_total", precision = 14, scale = 2)
    private BigDecimal grandTotal;

    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "INR";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
