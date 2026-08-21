package com.cbec.ai.pipeline.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "hs_raw")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HsRawEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_id", nullable = false)
    private Long executionId;

    @Column(name = "customs_territory", nullable = false, length = 50)
    private String customsTerritory;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "raw_national_code", length = 100)
    private String rawNationalCode;

    @Column(name = "raw_description", columnDefinition = "TEXT")
    private String rawDescription;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "source_name", length = 150)
    private String sourceName;

    @Column(name = "dataset_version", length = 50)
    private String datasetVersion;

    @Column(name = "extracted_at", nullable = false, updatable = false)
    private LocalDateTime extractedAt;

    @PrePersist
    protected void onCreate() {
        this.extractedAt = LocalDateTime.now();
    }
}
