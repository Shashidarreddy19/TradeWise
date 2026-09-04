package com.trade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Port {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 10, unique = true)
    private String unlocode;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(nullable = false, length = 2)
    private String countryCode;

    @Column(nullable = false, length = 50)
    private String portType; // SEA, AIR, ICD, DRY_PORT, RAIL_TERMINAL

    @Column(name = "annual_teu_capacity")
    private Long annualTeuCapacity;

    @Column(name = "congestion_level", length = 20)
    private String congestionLevel; // LOW, MODERATE, HIGH, SEVERE

    @Column(name = "avg_waiting_days")
    private Double avgWaitingDays;

    @Column(name = "terminal_operators", columnDefinition = "TEXT")
    private String terminalOperators;

    @Column(name = "customs_office_code", length = 50)
    private String customsOfficeCode;

    @Column(name = "nearby_icds", columnDefinition = "TEXT")
    private String nearbyIcds;

    @Column(name = "rail_connectivity")
    private Boolean railConnectivity;

    @Column(name = "road_connectivity")
    private Boolean roadConnectivity;

    @Column(length = 100)
    private String source;

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
