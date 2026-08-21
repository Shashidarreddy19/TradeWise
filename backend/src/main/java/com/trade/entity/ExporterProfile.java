package com.trade.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Extended profile for users with the EXPORTER role.
 * Contains business-specific information about the exporting entity.
 */
@Entity
@Table(name = "exporter_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExporterProfile {

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
     * Importer Exporter Code — mandatory government-issued code for Indian exporters.
     */
    @Column(name = "iec_code")
    private String iecCode;

    /**
     * GSTIN (India) or Business Registration Number (international).
     * Unique constraint prevents duplicate registrations.
     */
    @Column(name = "gst_number", unique = true)
    private String gstNumber;

    @Column(columnDefinition = "TEXT")
    private String address;

    // ── New fields added for production-grade registration ────────────────

    /**
     * Business type: Manufacturer, Trader, or Both.
     */
    @Column(name = "business_type", length = 30)
    private String businessType;

    /**
     * Export experience level: Beginner, Intermediate, Experienced.
     */
    @Column(name = "export_experience", length = 30)
    private String exportExperience;

    /**
     * Primary product description (10–500 chars).
     */
    @Column(name = "product_description", columnDefinition = "TEXT")
    private String productDescription;

    /**
     * Comma-separated product categories. Stored as CSV for simplicity;
     * maximum 3 selections from the defined list.
     * e.g. "Spices,Ceramics & Pottery,Others"
     */
    @Column(name = "product_categories", columnDefinition = "TEXT")
    private String productCategories;

    /**
     * Base country ISO name (same as user.country, denormalized for querying).
     */
    @Column(name = "base_country", length = 60)
    private String baseCountry;
}
