package com.cbec.ai.pipeline.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "regulation_hs_evidence_verification", uniqueConstraints = {
        @UniqueConstraint(name = "uq_reg_hs_evid_verif", columnNames = {"country", "hs_code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegulationHsEvidenceVerificationEntity {

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

    @Column(name = "regulation_id")
    private Long regulationId;

    @Column(name = "mapping_method", length = 50)
    private String mappingMethod;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(name = "source_reference", columnDefinition = "TEXT")
    private String sourceReference;

    @Column(name = "evidence_text", columnDefinition = "TEXT")
    private String evidenceText;

    @Column(name = "evidence_location", columnDefinition = "TEXT")
    private String evidenceLocation;

    @Column(name = "evidence_level", length = 50)
    private String evidenceLevel;

    @Column(name = "verification_status", length = 50)
    private String verificationStatus;

    @Column(name = "verification_reason", columnDefinition = "TEXT")
    private String verificationReason;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @PrePersist
    @PreUpdate
    public void onSave() {
        if (verifiedAt == null) {
            verifiedAt = LocalDateTime.now();
        }
    }
}
