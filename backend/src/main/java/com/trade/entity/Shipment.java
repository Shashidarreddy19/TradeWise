package com.trade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a physical shipment managed by a logistics partner.
 * Auto-created when an exporter accepts a logistics proposal.
 */
@Entity
@Table(name = "shipments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // One shipment tied to one order
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    // Logistics partner who accepted and owns this shipment
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logistics_partner_id", nullable = false)
    private User logisticsPartner;

    // Proposal upon which this shipment was created
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id")
    private LogisticsProposal proposal;

    @Column(name = "tracking_number", unique = true)
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipment_status", nullable = false)
    @Builder.Default
    private ShipmentStatus shipmentStatus = ShipmentStatus.ASSIGNED;

    // Port/city of origin — typically the pickup / port location
    @Column
    private String origin;

    // Final destination — city and country of delivery
    @Column
    private String destination;

    // Selected services for this shipment
    @Column(name = "services", length = 500)
    private String services;

    // Agreed total freight cost
    @Column(name = "cost", precision = 14, scale = 2)
    private BigDecimal cost;

    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "INR";

    // Cargo pickup date
    @Column(name = "pickup_date")
    private LocalDate pickupDate;

    // Estimated delivery date at destination
    @Column(name = "estimated_delivery")
    private LocalDate estimatedDelivery;

    // Timestamp when status was last updated
    @Column(name = "status_updated_at")
    private LocalDateTime statusUpdatedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Historical tracking milestones
    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt DESC")
    @Builder.Default
    private List<ShipmentTracking> trackingEvents = new ArrayList<>();
}
