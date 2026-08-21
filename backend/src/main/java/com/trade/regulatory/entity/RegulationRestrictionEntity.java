package com.trade.regulatory.entity;

import jakarta.persistence.*;

/**
 * Maps to tradedata.regulation_restrictions — restrictions per regulation.
 */
@Entity
@Table(name = "regulation_restrictions")
public class RegulationRestrictionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "regulation_id", nullable = false)
    private Long regulationId;

    @Column(name = "restriction_type", nullable = false, length = 100)
    private String restrictionType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    public Long getId() { return id; }
    public Long getRegulationId() { return regulationId; }
    public String getRestrictionType() { return restrictionType; }
    public String getDescription() { return description; }
    public String getRemarks() { return remarks; }
}
