package com.trade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "insurance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String policyType; // MARINE_CARGO_ICC_A, MARINE_CARGO_ICC_B, TRANSIT_ALL_RISKS

    @Column(nullable = false, length = 100)
    private String providerName;

    @Column(name = "premium_rate_pct", precision = 5, scale = 3)
    private BigDecimal premiumRatePct;

    @Column(name = "min_premium", precision = 10, scale = 2)
    private BigDecimal minPremium;

    @Column(name = "coverage_description", columnDefinition = "TEXT")
    private String coverageDescription;

    @Column(name = "claims_procedure", columnDefinition = "TEXT")
    private String claimsProcedure;

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
