package com.cbec.ai.pipeline.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "regulation_hs_mapping")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegulationHsMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "regulation_id", nullable = false)
    private Long regulationId;

    @Column(name = "chapter", length = 50)
    private String chapter;

    @Column(name = "heading", length = 255)
    private String heading;

    @Column(name = "hs6", length = 50)
    private String hs6;

    @Column(name = "national_code", length = 100)
    private String nationalCode;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "mapping_method", length = 50)
    private String mappingMethod;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "source_reference", columnDefinition = "TEXT")
    private String sourceReference;
}
