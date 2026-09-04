package com.trade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "freight_rates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FreightRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String mode; // ROAD, RAIL, AIR, SEA_FCL, SEA_LCL

    @Column(name = "origin_location", nullable = false, length = 100)
    private String originLocation;

    @Column(name = "destination_location", nullable = false, length = 100)
    private String destinationLocation;

    @Column(name = "base_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseRate;

    @Column(name = "rate_unit", nullable = false, length = 20)
    private String rateUnit; // PER_KG, PER_CBM, PER_20FT, PER_40FT, PER_CONTAINER

    @Column(name = "baf_surcharge", precision = 10, scale = 2)
    private BigDecimal bafSurcharge;

    @Column(name = "caf_surcharge", precision = 10, scale = 2)
    private BigDecimal cafSurcharge;

    @Column(name = "fuel_surcharge_pct", precision = 5, scale = 2)
    private BigDecimal fuelSurchargePct;

    @Column(name = "terminal_handling_charges", precision = 10, scale = 2)
    private BigDecimal terminalHandlingCharges;

    @Column(name = "documentation_fee", precision = 10, scale = 2)
    private BigDecimal documentationFee;

    @Column(name = "avg_transit_days")
    private Integer avgTransitDays;

    @Column(length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
