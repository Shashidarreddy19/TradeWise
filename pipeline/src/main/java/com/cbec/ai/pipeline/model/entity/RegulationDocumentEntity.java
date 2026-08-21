package com.cbec.ai.pipeline.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "regulation_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegulationDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "regulation_id", nullable = false)
    private Long regulationId;

    @Column(name = "document_name", nullable = false, length = 255)
    private String documentName;

    @Column(name = "mandatory")
    @Builder.Default
    private Boolean mandatory = true;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}
