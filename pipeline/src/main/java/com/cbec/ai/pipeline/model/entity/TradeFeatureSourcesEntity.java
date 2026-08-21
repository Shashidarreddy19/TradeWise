package com.cbec.ai.pipeline.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "trade_feature_sources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeFeatureSourcesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "feature_name", nullable = false, length = 100)
    private String featureName;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "hs6", length = 10)
    private String hs6;

    @Column(name = "source_name", nullable = false, length = 150)
    private String sourceName;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(name = "retrieval_date", updatable = false)
    private LocalDateTime retrievalDate;

    @Column(name = "reference_year")
    private Integer referenceYear;

    @Column(name = "methodology", columnDefinition = "TEXT")
    private String methodology;

    @Column(name = "confidence_status", length = 50)
    private String confidenceStatus;

    @PrePersist
    protected void onCreate() {
        if (retrievalDate == null) retrievalDate = LocalDateTime.now();
    }
}
