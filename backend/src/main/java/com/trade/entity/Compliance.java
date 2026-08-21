package com.trade.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Stores compliance and trade regulatory information for a specific
 * country + product category combination.
 *
 * This table is the data source for the /api/market-analysis endpoint.
 * It is designed to later serve as context for a RAG pipeline without
 * schema changes — the text fields map naturally to retrievable chunks.
 */
@Entity
@Table(
    name = "compliance",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_compliance_country_category",
            columnNames = {"country_id", "category_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Compliance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategory category;

    /**
     * Comma-separated list of required certificates.
     * e.g. "Phytosanitary Certificate, Organic Certificate, FSSAI Export License"
     */
    @Column(name = "required_certificates", columnDefinition = "TEXT")
    private String requiredCertificates;

    /**
     * Comma-separated list of required shipping/customs documents.
     * e.g. "Commercial Invoice, Packing List, Bill of Lading, Certificate of Origin"
     */
    @Column(name = "required_documents", columnDefinition = "TEXT")
    private String requiredDocuments;

    /**
     * Import/customs duty percentage applied at destination.
     * Stored as decimal — e.g. 5.00 means 5%.
     */
    @Column(name = "customs_duty", precision = 5, scale = 2)
    private java.math.BigDecimal customsDuty;

    /**
     * Comma-separated list of restricted items or conditions.
     * e.g. "Genetically Modified Organisms (GMO), products without EU organic label"
     */
    @Column(name = "restricted_items", columnDefinition = "TEXT")
    private String restrictedItems;

    /**
     * Typical transit time in days from India to this country.
     */
    @Column(name = "transit_time")
    private String transitTime;

    /**
     * Recommended port of entry in the destination country.
     * e.g. "Hamburg Port, Germany"
     */
    @Column(name = "recommended_port")
    private String recommendedPort;
}
