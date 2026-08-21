package com.cbec.ai.pipeline.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "regulation_evidence_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegulationEvidenceAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "regulation_id", nullable = false)
    private Long regulationId;

    @Column(name = "requirement_type", nullable = false, length = 50)
    private String requirementType;

    @Column(name = "requirement_id", nullable = false)
    private Long requirementId;

    @Column(name = "validation_status", nullable = false, length = 50)
    private String validationStatus;

    @Column(name = "source_evidence", columnDefinition = "TEXT")
    private String sourceEvidence;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "audit_timestamp", updatable = false)
    private LocalDateTime auditTimestamp;

    @PrePersist
    protected void onCreate() {
        if (auditTimestamp == null) {
            auditTimestamp = LocalDateTime.now();
        }
    }
}
