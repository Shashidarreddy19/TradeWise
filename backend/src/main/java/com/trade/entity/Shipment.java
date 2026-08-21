package com.trade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a physical shipment managed by a logistics partner.
 * Auto-created when a logistics partner accepts an Export Shipment Request.
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

    @Column(name = "tracking_number", unique = true)
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipment_status", nullable = false)
    @Builder.Default
    private ShipmentStatus shipmentStatus = ShipmentStatus.ASSIGNED;

    // Port/city of origin — typically the Indian export port
    @Column
    private String origin;

    // Final destination — city and country of delivery
    @Column
    private String destination;

    // Estimated delivery date at destination
    @Column(name = "estimated_delivery")
    private LocalDate estimatedDelivery;

    // Timestamp when status was last updated
    @Column(name = "status_updated_at")
    private LocalDateTime statusUpdatedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
