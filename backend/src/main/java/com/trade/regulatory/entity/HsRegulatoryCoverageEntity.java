package com.trade.regulatory.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Maps to tradedata.hs_regulatory_coverage_audit — full regulatory coverage matrix.
 * 11,500 records covering India's HS codes with regulatory mapping status.
 */
@Entity
@Table(name = "hs_regulatory_coverage_audit")
public class HsRegulatoryCoverageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(name = "hs_code", nullable = false, length = 50)
    private String hsCode;

    @Column(length = 2)
    private String hs2;

    @Column(length = 4)
    private String hs4;

    @Column(length = 6)
    private String hs6;

    @Column(name = "national_code", length = 50)
    private String nationalCode;

    @Column(name = "product_description", columnDefinition = "TEXT")
    private String productDescription;

    @Column(name = "regulation_found")
    private Boolean regulationFound;

    @Column(name = "regulation_id")
    private Long regulationId;

    @Column(name = "regulation_title", columnDefinition = "TEXT")
    private String regulationTitle;

    @Column(name = "regulation_type", length = 100)
    private String regulationType;

    @Column(length = 255)
    private String authority;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(name = "source_reference", columnDefinition = "TEXT")
    private String sourceReference;

    @Column(name = "mapping_method", length = 50)
    private String mappingMethod;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "documents_found")
    private Integer documentsFound;

    @Column(name = "certifications_found")
    private Integer certificationsFound;

    @Column(name = "restrictions_found")
    private Integer restrictionsFound;

    @Column(name = "labeling_found")
    private Integer labelingFound;

    @Column(name = "customs_procedure_found")
    private Integer customsProcedureFound;

    @Column(name = "coverage_status", nullable = false, length = 50)
    private String coverageStatus;

    @Column(name = "audit_timestamp")
    private LocalDateTime auditTimestamp;

    // Getters
    public Long getId() { return id; }
    public String getCountry() { return country; }
    public String getHsCode() { return hsCode; }
    public String getHs2() { return hs2; }
    public String getHs4() { return hs4; }
    public String getHs6() { return hs6; }
    public String getNationalCode() { return nationalCode; }
    public String getProductDescription() { return productDescription; }
    public Boolean getRegulationFound() { return regulationFound; }
    public Long getRegulationId() { return regulationId; }
    public String getRegulationTitle() { return regulationTitle; }
    public String getAuthority() { return authority; }
    public String getSourceUrl() { return sourceUrl; }
    public String getMappingMethod() { return mappingMethod; }
    public Double getConfidenceScore() { return confidenceScore; }
    public Integer getDocumentsFound() { return documentsFound; }
    public Integer getCertificationsFound() { return certificationsFound; }
    public Integer getRestrictionsFound() { return restrictionsFound; }
    public Integer getLabelingFound() { return labelingFound; }
    public Integer getCustomsProcedureFound() { return customsProcedureFound; }
    public String getCoverageStatus() { return coverageStatus; }
}
