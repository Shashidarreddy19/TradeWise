package com.cbec.ai.pipeline.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "regulation_hs_mapping_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegulationHsMappingAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "mapping_id", nullable = false)
    private Long mappingId;

    @Column(name = "national_code", length = 100)
    private String nationalCode;

    @Column(name = "hs6", length = 50)
    private String hs6;

    @Column(name = "heading", length = 50)
    private String heading;

    @Column(name = "chapter", length = 50)
    private String chapter;

    @Column(name = "hierarchy_level", nullable = false, length = 50)
    private String hierarchyLevel;

    @Column(name = "valid_in_hs_master", nullable = false)
    private Boolean validInHsMaster;

    @Column(name = "validation_status", nullable = false, length = 50)
    private String validationStatus;

    @Column(name = "source_reference", columnDefinition = "TEXT")
    private String sourceReference;

    @Column(name = "audit_timestamp", updatable = false)
    private LocalDateTime auditTimestamp;

    @PrePersist
    protected void onCreate() {
        if (auditTimestamp == null) {
            auditTimestamp = LocalDateTime.now();
        }
    }
}
