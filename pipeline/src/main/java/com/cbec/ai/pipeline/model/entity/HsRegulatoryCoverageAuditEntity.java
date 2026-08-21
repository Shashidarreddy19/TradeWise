package com.cbec.ai.pipeline.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "hs_regulatory_coverage_audit", uniqueConstraints = {
        @UniqueConstraint(name = "uq_hs_reg_coverage", columnNames = {"country", "hs_code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HsRegulatoryCoverageAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "hs_code", nullable = false, length = 50)
    private String hsCode;

    @Column(name = "hs2", length = 2)
    private String hs2;

    @Column(name = "hs4", length = 4)
    private String hs4;

    @Column(name = "hs6", length = 6)
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

    @Column(name = "authority", length = 255)
    private String authority;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(name = "source_reference", columnDefinition = "TEXT")
    private String sourceReference;

    /**
     * Matching method used:
     * EXACT_NATIONAL_CODE | HS6 | HS4_HEADING | HS2_CHAPTER | NOT_FOUND
     */
    @Column(name = "mapping_method", length = 50)
    private String mappingMethod;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    // --- Per-category counts (populated only if regulation_found = true) ---

    @Column(name = "documents_found")
    private Integer documentsFound;

    @Column(name = "certifications_found")
    private Integer certificationsFound;

    @Column(name = "restrictions_found")
    private Integer restrictionsFound;

    @Column(name = "labeling_found")
    private Integer labelingFound;

    @Column(name = "packaging_found")
    private Integer packagingFound;

    @Column(name = "sps_found")
    private Integer spsFound;

    @Column(name = "tbt_found")
    private Integer tbtFound;

    @Column(name = "licensing_found")
    private Integer licensingFound;

    @Column(name = "inspection_found")
    private Integer inspectionFound;

    @Column(name = "origin_rules_found")
    private Integer originRulesFound;

    @Column(name = "sector_rules_found")
    private Integer sectorRulesFound;

    @Column(name = "customs_procedure_found")
    private Integer customsProcedureFound;

    @Column(name = "tariff_found")
    private Boolean tariffFound;

    @Column(name = "evidence_complete")
    private Boolean evidenceComplete;

    @Column(name = "source_count")
    private Integer sourceCount;

    /**
     * Coverage status:
     * COMPLETE | PARTIAL | NO_SPECIFIC_REGULATION_FOUND | DATA_UNAVAILABLE
     */
    @Column(name = "coverage_status", nullable = false, length = 50)
    private String coverageStatus;

    @Column(name = "effective_date")
    private LocalDateTime effectiveDate;

    @Column(name = "retrieved_at", updatable = false)
    private LocalDateTime retrievedAt;

    @Column(name = "audit_timestamp", updatable = false)
    private LocalDateTime auditTimestamp;

    @PrePersist
    protected void onCreate() {
        if (auditTimestamp == null) auditTimestamp = LocalDateTime.now();
        if (retrievedAt == null) retrievedAt = LocalDateTime.now();
        if (documentsFound == null) documentsFound = 0;
        if (certificationsFound == null) certificationsFound = 0;
        if (restrictionsFound == null) restrictionsFound = 0;
        if (labelingFound == null) labelingFound = 0;
        if (packagingFound == null) packagingFound = 0;
        if (spsFound == null) spsFound = 0;
        if (tbtFound == null) tbtFound = 0;
        if (licensingFound == null) licensingFound = 0;
        if (inspectionFound == null) inspectionFound = 0;
        if (originRulesFound == null) originRulesFound = 0;
        if (sectorRulesFound == null) sectorRulesFound = 0;
        if (customsProcedureFound == null) customsProcedureFound = 0;
        if (sourceCount == null) sourceCount = 0;
        if (regulationFound == null) regulationFound = false;
        if (tariffFound == null) tariffFound = false;
        if (evidenceComplete == null) evidenceComplete = false;
    }
}
