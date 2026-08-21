package com.trade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Tracks which logistics partners have rejected a specific shipment request.
 * Used to keep the request available to other partners after a rejection,
 * and to determine when all partners have rejected it.
 */
@Entity
@Table(
    name = "order_rejections",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_order_rejection",
            columnNames = {"order_id", "logistics_partner_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRejection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logistics_partner_id", nullable = false)
    private User logisticsPartner;

    @Column(name = "rejected_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime rejectedAt;
}
