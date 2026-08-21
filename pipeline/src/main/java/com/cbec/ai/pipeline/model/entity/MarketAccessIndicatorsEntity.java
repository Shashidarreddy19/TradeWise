package com.cbec.ai.pipeline.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "market_access_indicators", uniqueConstraints = {
        @UniqueConstraint(name = "uq_market_access_key", columnNames = {"country", "trade_year"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketAccessIndicatorsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "trade_year", nullable = false)
    private Integer year;

    @Column(name = "market_access_score")
    private Integer marketAccessScore;

    @Column(name = "pta_indicator")
    private Boolean ptaIndicator;

    @Column(name = "india_fta_indicator")
    private Boolean indiaFtaIndicator;

    @Column(name = "agreement_name", length = 255)
    private String agreementName;

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
