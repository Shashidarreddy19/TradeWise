package com.trade.regulatory.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Maps to tradedata.country_master — supported destination countries.
 */
@Entity
@Table(name = "country_master")
public class CountryMasterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "country_code", nullable = false, unique = true, length = 10)
    private String countryCode;

    @Column(name = "country_name", nullable = false, unique = true, length = 100)
    private String countryName;

    @Column(name = "customs_territory", nullable = false, length = 50)
    private String customsTerritory;

    @Column
    private Boolean active;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public String getCountryCode() { return countryCode; }
    public String getCountryName() { return countryName; }
    public String getCustomsTerritory() { return customsTerritory; }
    public Boolean getActive() { return active; }
}
