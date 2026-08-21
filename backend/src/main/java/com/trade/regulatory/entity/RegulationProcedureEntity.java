package com.trade.regulatory.entity;

import jakarta.persistence.*;

/**
 * Maps to tradedata.regulation_procedures — customs/import procedures.
 */
@Entity
@Table(name = "regulation_procedures")
public class RegulationProcedureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "regulation_id", nullable = false)
    private Long regulationId;

    @Column(name = "procedure_name", nullable = false, length = 255)
    private String procedureName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "step_order")
    private Integer stepOrder;

    public Long getId() { return id; }
    public Long getRegulationId() { return regulationId; }
    public String getProcedureName() { return procedureName; }
    public String getDescription() { return description; }
    public Integer getStepOrder() { return stepOrder; }
}
