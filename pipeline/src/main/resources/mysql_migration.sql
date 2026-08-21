-- CBEC-AI MySQL Migration Script
-- Creates missing tables not yet in TradeData database
-- All uses CREATE TABLE IF NOT EXISTS (safe to run multiple times)
-- Indexes use MySQL-compatible syntax (no IF NOT EXISTS for INDEX)

USE TradeData;

-- =====================================================================
-- MISSING TABLE 1: regulation_data_quality_audit
-- =====================================================================
CREATE TABLE IF NOT EXISTS regulation_data_quality_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    country VARCHAR(100) NOT NULL,
    regulation_id BIGINT,
    audit_type VARCHAR(100),
    field_name VARCHAR(100),
    issue_type VARCHAR(100),
    issue_description TEXT,
    severity VARCHAR(20) DEFAULT 'INFO',
    resolved BOOLEAN DEFAULT FALSE,
    audit_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================================
-- MISSING TABLE 2: regulation_evidence_audit
-- =====================================================================
CREATE TABLE IF NOT EXISTS regulation_evidence_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    country VARCHAR(100) NOT NULL,
    regulation_id BIGINT,
    source_url TEXT,
    source_reference TEXT,
    sha256 VARCHAR(64),
    evidence_type VARCHAR(100),
    evidence_status VARCHAR(50) DEFAULT 'VERIFIED',
    verified_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    audit_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================================
-- MISSING TABLE 3: regulation_hs_mapping_audit
-- =====================================================================
CREATE TABLE IF NOT EXISTS regulation_hs_mapping_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    country VARCHAR(100) NOT NULL,
    mapping_id BIGINT,
    regulation_id BIGINT,
    hs_code VARCHAR(50),
    mapping_method VARCHAR(50),
    confidence_score DOUBLE,
    audit_status VARCHAR(50) DEFAULT 'VERIFIED',
    audit_notes TEXT,
    audit_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================================
-- MISSING TABLE 4: market_trade_features
-- =====================================================================
CREATE TABLE IF NOT EXISTS market_trade_features (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hs6 VARCHAR(10) NOT NULL,
    destination_country VARCHAR(100) NOT NULL,
    trade_year INT NOT NULL,
    import_value_usd DOUBLE,
    india_export_value_usd DOUBLE,
    india_market_share_percent DOUBLE,
    yoy_growth_percent DOUBLE,
    import_growth_percent DOUBLE,
    market_size_score INT,
    mfn_tariff_percent DOUBLE,
    preferential_tariff_percent DOUBLE,
    effective_tariff_percent DOUBLE,
    tariff_advantage_score INT,
    supplier_hhi DOUBLE,
    top_supplier_share DOUBLE,
    number_of_supplier_countries INT,
    competition_score INT,
    dataset_status VARCHAR(50) DEFAULT 'DATA_AVAILABLE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_market_trade_hs6_cty_yr UNIQUE (hs6, destination_country, trade_year)
);

-- =====================================================================
-- MISSING TABLE 5: country_economic_indicators
-- =====================================================================
CREATE TABLE IF NOT EXISTS country_economic_indicators (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    country VARCHAR(100) NOT NULL,
    indicator_year INT NOT NULL,
    gdp_usd_billion DOUBLE,
    gdp_growth_percent DOUBLE,
    inflation_percent DOUBLE,
    fdi_inflow_usd_billion DOUBLE,
    ease_of_doing_business_rank INT,
    logistics_performance_index DOUBLE,
    corruption_perception_index DOUBLE,
    trade_openness_percent DOUBLE,
    currency_code VARCHAR(10),
    exchange_rate_to_usd DOUBLE,
    source_reference TEXT,
    dataset_status VARCHAR(50) DEFAULT 'DATA_AVAILABLE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_econ_country_year UNIQUE (country, indicator_year)
);

-- =====================================================================
-- MISSING TABLE 6: cbec_country_recommendation_dataset
-- =====================================================================
CREATE TABLE IF NOT EXISTS cbec_country_recommendation_dataset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hs6 VARCHAR(10) NOT NULL,
    destination_country VARCHAR(100) NOT NULL,
    trade_year INT NOT NULL,
    -- Trade Features
    import_value_usd DOUBLE,
    india_export_value_usd DOUBLE,
    india_market_share_percent DOUBLE,
    yoy_growth_percent DOUBLE,
    import_growth_percent DOUBLE,
    market_size_score INT,
    -- Tariff Features
    mfn_tariff_percent DOUBLE,
    preferential_tariff_percent DOUBLE,
    effective_tariff_percent DOUBLE,
    tariff_advantage_score INT,
    -- Competition Features
    supplier_hhi DOUBLE,
    top_supplier_share DOUBLE,
    number_of_supplier_countries INT,
    competition_score INT,
    -- Regulatory Features
    regulation_count INT DEFAULT 0,
    document_requirements INT DEFAULT 0,
    certification_requirements INT DEFAULT 0,
    restriction_count INT DEFAULT 0,
    labeling_requirements INT DEFAULT 0,
    sps_requirements INT DEFAULT 0,
    tbt_requirements INT DEFAULT 0,
    regulatory_complexity_score INT DEFAULT 0,
    -- Economic Features
    gdp_usd_billion DOUBLE,
    gdp_growth_percent DOUBLE,
    inflation_percent DOUBLE,
    ease_of_doing_business_rank INT,
    logistics_performance_index DOUBLE,
    -- Source Attribution Metadata
    trade_source VARCHAR(150),
    tariff_source VARCHAR(150),
    economic_source VARCHAR(150),
    risk_source VARCHAR(150),
    dataset_status VARCHAR(50) DEFAULT 'DATA_AVAILABLE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_cbec_dataset_key UNIQUE (hs6, destination_country, trade_year)
);

-- =====================================================================
-- MISSING TABLE 7: hs_regulatory_coverage_audit (NEW - just added)
-- =====================================================================
CREATE TABLE IF NOT EXISTS hs_regulatory_coverage_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    country VARCHAR(100) NOT NULL,
    hs_code VARCHAR(50) NOT NULL,
    hs2 VARCHAR(2),
    hs4 VARCHAR(4),
    hs6 VARCHAR(6),
    national_code VARCHAR(50),
    product_description TEXT,
    regulation_found BOOLEAN DEFAULT FALSE,
    regulation_id BIGINT,
    regulation_title TEXT,
    regulation_type VARCHAR(100),
    authority VARCHAR(255),
    source_url TEXT,
    source_reference TEXT,
    mapping_method VARCHAR(50),
    confidence_score DOUBLE,
    documents_found INT DEFAULT 0,
    certifications_found INT DEFAULT 0,
    restrictions_found INT DEFAULT 0,
    labeling_found INT DEFAULT 0,
    packaging_found INT DEFAULT 0,
    sps_found INT DEFAULT 0,
    tbt_found INT DEFAULT 0,
    licensing_found INT DEFAULT 0,
    inspection_found INT DEFAULT 0,
    origin_rules_found INT DEFAULT 0,
    sector_rules_found INT DEFAULT 0,
    customs_procedure_found INT DEFAULT 0,
    tariff_found BOOLEAN DEFAULT FALSE,
    evidence_complete BOOLEAN DEFAULT FALSE,
    source_count INT DEFAULT 0,
    coverage_status VARCHAR(50) NOT NULL,
    effective_date DATETIME,
    retrieved_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    audit_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_hs_reg_coverage UNIQUE (country, hs_code)
);

-- =====================================================================
-- INDEXES (MySQL compatible — no IF NOT EXISTS for CREATE INDEX)
-- =====================================================================

-- regulation_data_quality_audit
CREATE INDEX idx_rdqa_country ON regulation_data_quality_audit(country);

-- regulation_evidence_audit
CREATE INDEX idx_rea_country ON regulation_evidence_audit(country);

-- market_trade_features
CREATE INDEX idx_mtf_hs6_cty ON market_trade_features(hs6, destination_country);

-- country_economic_indicators
CREATE INDEX idx_cei_country ON country_economic_indicators(country);

-- cbec_country_recommendation_dataset
CREATE INDEX idx_cbec_ds_hs6_cty ON cbec_country_recommendation_dataset(hs6, destination_country);

-- hs_regulatory_coverage_audit
CREATE INDEX idx_hs_reg_cov_country ON hs_regulatory_coverage_audit(country);
CREATE INDEX idx_hs_reg_cov_hs6 ON hs_regulatory_coverage_audit(hs6);
CREATE INDEX idx_hs_reg_cov_status ON hs_regulatory_coverage_audit(coverage_status);
CREATE INDEX idx_hs_reg_cov_cty_status ON hs_regulatory_coverage_audit(country, coverage_status);
