package com.cbec.ai.pipeline.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "country_risk_indicators", uniqueConstraints = {
        @UniqueConstraint(name = "uq_country_risk_key", columnNames = {"country", "trade_year"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CountryRiskIndicatorsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "trade_year", nullable = false)
    private Integer year;

    @Column(name = "country_risk_score")
    private Integer countryRiskScore;

    @Column(name = "political_risk_indicator", length = 50)
    private String politicalRiskIndicator;

    @Column(name = "economic_risk_indicator", length = 50)
    private String economicRiskIndicator;

    @Column(name = "trade_risk_indicator", length = 50)
    private String tradeRiskIndicator;

    @Column(name = "source", nullable = false, length = 150)
    private String source;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
