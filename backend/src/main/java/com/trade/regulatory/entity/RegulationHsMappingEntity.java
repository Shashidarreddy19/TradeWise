package com.trade.regulatory.entity;

import jakarta.persistence.*;

/**
 * Maps to tradedata.regulation_hs_mapping — links regulations to HS codes
 * with hierarchical matching levels.
 */
@Entity
@Table(name = "regulation_hs_mapping")
public class RegulationHsMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "regulation_id", nullable = false)
    private Long regulationId;

    @Column(length = 50)
    private String chapter;

    @Column(length = 255)
    private String heading;

    @Column(length = 50)
    private String hs6;

    @Column(name = "national_code", length = 100)
    private String nationalCode;

    @Column
    private Double confidence;

    @Column(name = "mapping_method", length = 50)
    private String mappingMethod;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "source_reference", columnDefinition = "TEXT")
    private String sourceReference;

    public Long getId() { return id; }
    public Long getRegulationId() { return regulationId; }
    public String getChapter() { return chapter; }
    public String getHeading() { return heading; }
    public String getHs6() { return hs6; }
    public String getNationalCode() { return nationalCode; }
    public String getMappingMethod() { return mappingMethod; }
    public Double getConfidenceScore() { return confidenceScore; }
    public String getSourceReference() { return sourceReference; }
}
