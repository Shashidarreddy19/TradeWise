package com.trade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "carriers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Carrier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String carrierType; // OCEAN, AIR, ROAD, RAIL

    @Column(length = 20)
    private String code; // SCAC / IATA code e.g. MAEU, EK, AI

    @Column(name = "reliability_score", precision = 5, scale = 2)
    private BigDecimal reliabilityScore; // 0 to 100

    @Column(name = "historic_on_time_pct", precision = 5, scale = 2)
    private BigDecimal historicOnTimePct;

    @Column(name = "cost_rating", length = 20)
    private String costRating; // BUDGET, COMPETITIVE, PREMIUM

    @Column(name = "service_lanes", columnDefinition = "TEXT")
    private String serviceLanes;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

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
