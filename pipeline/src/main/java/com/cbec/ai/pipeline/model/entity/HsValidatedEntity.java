package com.cbec.ai.pipeline.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "hs_validated")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HsValidatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_id", nullable = false)
    private Long executionId;

    @Column(name = "customs_territory", nullable = false, length = 50)
    private String customsTerritory;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "chapter", nullable = false, length = 2)
    private String chapter;

    @Column(name = "heading", nullable = false, length = 4)
    private String heading;

    @Column(name = "hs6", nullable = false, length = 6)
    private String hs6;

    @Column(name = "national_code", nullable = false, length = 50)
    private String nationalCode;

    @Column(name = "code_length", nullable = false)
    private Integer codeLength;

    @Column(name = "nomenclature_type", nullable = false, length = 50)
    private String nomenclatureType;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Column(name = "official_description", nullable = false, columnDefinition = "TEXT")
    private String officialDescription;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "source_name", length = 150)
    private String sourceName;

    @Column(name = "dataset_version", length = 50)
    private String datasetVersion;

    @Column(name = "validated_at", nullable = false, updatable = false)
    private LocalDateTime validatedAt;

    @PrePersist
    protected void onCreate() {
        this.validatedAt = LocalDateTime.now();
    }
}
