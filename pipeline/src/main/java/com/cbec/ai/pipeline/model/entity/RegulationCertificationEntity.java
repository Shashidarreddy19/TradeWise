package com.cbec.ai.pipeline.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "regulation_certifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegulationCertificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "regulation_id", nullable = false)
    private Long regulationId;

    @Column(name = "certification_name", nullable = false, length = 255)
    private String certificationName;

    @Column(name = "mandatory")
    @Builder.Default
    private Boolean mandatory = true;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}
