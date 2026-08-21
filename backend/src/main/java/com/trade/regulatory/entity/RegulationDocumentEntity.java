package com.trade.regulatory.entity;

import jakarta.persistence.*;

/**
 * Maps to tradedata.regulation_documents — required documents per regulation.
 */
@Entity
@Table(name = "regulation_documents")
public class RegulationDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "regulation_id", nullable = false)
    private Long regulationId;

    @Column(name = "document_name", nullable = false, length = 255)
    private String documentName;

    @Column
    private Boolean mandatory;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    public Long getId() { return id; }
    public Long getRegulationId() { return regulationId; }
    public String getDocumentName() { return documentName; }
    public Boolean getMandatory() { return mandatory; }
    public String getRemarks() { return remarks; }
}
