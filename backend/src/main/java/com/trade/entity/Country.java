package com.trade.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a supported export destination country.
 * Maps to the shared InternationalTrade.countries table (also used by the ML service).
 * Seeded at application startup via DataSeeder.
 *
 * Column notes (must match the ML service's countries table):
 *   code        VARCHAR(2)  NOT NULL UNIQUE  — ISO-2 code, e.g. DE, US, AE
 *   name        VARCHAR(100) NOT NULL
 *   currency    VARCHAR(10)  — application-specific, nullable in shared table
 *   region      VARCHAR(50)  nullable
 *   trade_agreement VARCHAR(100) nullable
 *   active      BOOLEAN default true
 */
@Entity
@Table(name = "countries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ISO-2 country code, e.g. DE, US, AE, SG, AU. NOT NULL in the shared table. */
    @Column(nullable = false, unique = true, length = 2)
    private String code;

    @Column(nullable = false)
    private String name;

    /** ISO 4217 currency code, e.g. EUR, USD. Nullable in the shared schema. */
    @Column(length = 10)
    private String currency;

    @Column(length = 50)
    private String region;

    @Column(name = "trade_agreement", length = 100)
    private String tradeAgreement;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    @Builder.Default
    private Boolean active = true;
}
