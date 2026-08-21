package com.trade.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Extended profile for users with the LOGISTICS role.
 * Contains freight and operational information for logistics partners.
 */
@Entity
@Table(name = "logistics_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogisticsProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // One-to-one with User
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    /**
     * Comma-separated service types offered.
     * e.g. "Air Freight,Sea Freight,Customs Clearance"
     */
    @Column(name = "services", columnDefinition = "TEXT")
    private String services;

    /**
     * Geographic regions / trade lanes this partner covers.
     * e.g. "Middle East, Southeast Asia, Europe"
     */
    @Column(name = "service_area", columnDefinition = "TEXT")
    private String serviceArea;

    /**
     * Business Registration Number (GSTIN or equivalent).
     */
    @Column(name = "business_registration_number", length = 20)
    private String businessRegistrationNumber;

    /**
     * Years of experience in logistics.
     * e.g. "3-5 years", "10+ years"
     */
    @Column(name = "experience", length = 30)
    private String experience;

    /**
     * Whether the partner provides live shipment tracking.
     */
    @Column(name = "tracking_support")
    @Builder.Default
    private Boolean trackingSupport = false;

    /**
     * Whether the partner provides cargo insurance.
     */
    @Column(name = "cargo_insurance")
    @Builder.Default
    private Boolean cargoInsurance = false;

    /**
     * Legacy field — retained for backward compatibility.
     * Now replaced by 'experience' for display purposes.
     */
    @Column(name = "fleet_size")
    private String fleetSize;
}
