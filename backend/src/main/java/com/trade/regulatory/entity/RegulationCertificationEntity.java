package com.trade.regulatory.entity;

import jakarta.persistence.*;

/**
 * Maps to tradedata.regulation_certifications — required certifications per regulation.
 */
@Entity
@Table(name = "regulation_certifications")
public class RegulationCertificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "regulation_id", nullable = false)
    private Long regulationId;

    @Column(name = "certification_name", nullable = false, length = 255)
    private String certificationName;

    @Column
    private Boolean mandatory;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    public Long getId() { return id; }
    public Long getRegulationId() { return regulationId; }
    public String getCertificationName() { return certificationName; }
    public Boolean getMandatory() { return mandatory; }
    public String getRemarks() { return remarks; }
}
