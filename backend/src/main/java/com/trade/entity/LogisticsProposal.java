package com.trade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Quotation / Service Proposal submitted by a logistics provider for an export order.
 */
@Entity
@Table(name = "logistics_proposals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogisticsProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Export order for which this quote/proposal is submitted
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // The logistics provider who submitted this proposal
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logistics_partner_id", nullable = false)
    private User logisticsPartner;

    // Selected services (e.g. "Sea Freight, Customs Clearance, Warehousing")
    @Column(name = "services", nullable = false, length = 500)
    private String services;

    // Quoted / Estimated cost
    @Column(name = "estimated_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal estimatedCost;

    // Currency (default: INR)
    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "INR";

    // Estimated transit duration in days
    @Column(name = "estimated_transit_days")
    private Integer estimatedTransitDays;

    // Proposed cargo pickup date
    @Column(name = "pickup_date")
    private LocalDate pickupDate;

    // Expected delivery date at destination
    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    // Any port, terminal or insurance surcharge
    @Column(name = "additional_charges", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal additionalCharges = BigDecimal.ZERO;

    // Special notes, terms, route details, carrier info
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ProposalStatus status = ProposalStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
