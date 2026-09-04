package com.trade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "incoterms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incoterm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10, unique = true)
    private String code; // EXW, FCA, FOB, CFR, CIF, DAP, DDP, DPU

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "seller_responsibility", columnDefinition = "TEXT")
    private String sellerResponsibility;

    @Column(name = "buyer_responsibility", columnDefinition = "TEXT")
    private String buyerResponsibility;

    @Column(name = "insurance_responsibility", length = 100)
    private String insuranceResponsibility;

    @Column(name = "risk_transfer_point", columnDefinition = "TEXT")
    private String riskTransferPoint;

    @Column(name = "cost_transfer_point", columnDefinition = "TEXT")
    private String costTransferPoint;

    @Column(name = "required_documents", columnDefinition = "TEXT")
    private String requiredDocuments;

    @Column(length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
