package com.cbec.ai.pipeline.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "regulation_labeling")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegulationLabelingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "regulation_id", nullable = false)
    private Long regulationId;

    @Column(name = "requirement", nullable = false, columnDefinition = "TEXT")
    private String requirement;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}
