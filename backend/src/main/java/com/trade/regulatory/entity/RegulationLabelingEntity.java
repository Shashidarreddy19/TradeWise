package com.trade.regulatory.entity;

import jakarta.persistence.*;

/**
 * Maps to tradedata.regulation_labeling — labeling requirements per regulation.
 */
@Entity
@Table(name = "regulation_labeling")
public class RegulationLabelingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "regulation_id", nullable = false)
    private Long regulationId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String requirement;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    public Long getId() { return id; }
    public Long getRegulationId() { return regulationId; }
    public String getRequirement() { return requirement; }
    public String getRemarks() { return remarks; }
}
