package com.trade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents an Export Shipment Request created by an Exporter.
 *
 * Lifecycle:
 *  1. Exporter creates → status = PENDING_LOGISTICS
 *  2. Logistics partner accepts → status = LOGISTICS_ACCEPTED, shipment auto-created
 *  3. Shipment progresses → status mirrors shipment (IN_TRANSIT → DELIVERED)
 *  4. All logistics partners reject → status = REJECTED
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The product being requested for export
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // The exporter who created this request
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exporter_id", nullable = false)
    private User exporter;

    // Destination country
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_country_id", nullable = false)
    private Country destinationCountry;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "total_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalPrice;

    // Shipping preferences: e.g. "Sea Freight", "Air Freight", "Road"
    @Column(name = "shipping_requirements")
    private String shippingRequirements;

    // Any special notes from the exporter
    @Column(name = "special_instructions", columnDefinition = "TEXT")
    private String specialInstructions;

    // Pickup address for the logistics partner
    @Column(name = "pickup_location")
    private String pickupLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING_LOGISTICS;

    // Set when a logistics partner accepts this request
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_logistics_partner_id")
    private User assignedLogisticsPartner;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Set when a logistics partner accepts
    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    // Linked shipment — null until logistics partner accepts
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Shipment shipment;
}
