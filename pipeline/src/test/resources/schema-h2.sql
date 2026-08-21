-- H2 Test Database Setup Script
CREATE TABLE IF NOT EXISTS country_master (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    country_code VARCHAR(10) UNIQUE NOT NULL,
    country_name VARCHAR(100) UNIQUE NOT NULL,
    customs_territory VARCHAR(50) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS category_master (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    active BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS category_chapter (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    chapter VARCHAR(2) NOT NULL,
    FOREIGN KEY (category_id) REFERENCES category_master(id) ON DELETE CASCADE,
    UNIQUE (category_id, chapter)
);

CREATE TABLE IF NOT EXISTS source_master (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customs_territory VARCHAR(50) NOT NULL,
    country VARCHAR(100) NOT NULL,
    authority VARCHAR(150) NOT NULL,
    source_name VARCHAR(150) NOT NULL,
    download_url VARCHAR(500),
    documentation_url VARCHAR(500),
    source_type VARCHAR(30) NOT NULL,
    data_format VARCHAR(20) NOT NULL,
    nomenclature_type VARCHAR(50) NOT NULL,
    minimum_code_length INT NOT NULL DEFAULT 6,
    maximum_code_length INT NOT NULL DEFAULT 12,
    preferred_parser VARCHAR(50),
    authentication_type VARCHAR(30) DEFAULT 'NONE',
    checksum_algorithm VARCHAR(20) DEFAULT 'SHA-256',
    status VARCHAR(30) DEFAULT 'ACTIVE',
    supports_versioning BOOLEAN DEFAULT TRUE,
    supports_incremental_updates BOOLEAN DEFAULT FALSE,
    sync_frequency VARCHAR(20) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    last_successful_sync DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS download_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_id BIGINT,
    country VARCHAR(100) NOT NULL,
    source_name VARCHAR(150) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    content_type VARCHAR(100),
    file_size BIGINT NOT NULL,
    etag VARCHAR(255),
    last_modified VARCHAR(255),
    release_date DATETIME,
    checksum_algorithm VARCHAR(20) DEFAULT 'SHA-256',
    sha256 VARCHAR(64) NOT NULL,
    download_url VARCHAR(500),
    http_status INT,
    download_duration_ms BIGINT DEFAULT 0,
    dataset_version VARCHAR(50),
    downloaded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (source_id) REFERENCES source_master(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS pipeline_execution (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pipeline_name VARCHAR(150) NOT NULL,
    country VARCHAR(100) NOT NULL,
    source VARCHAR(100) NOT NULL,
    started_at DATETIME NOT NULL,
    completed_at DATETIME,
    records_found BIGINT DEFAULT 0,
    records_inserted BIGINT DEFAULT 0,
    records_updated BIGINT DEFAULT 0,
    duplicates BIGINT DEFAULT 0,
    invalid_records BIGINT DEFAULT 0,
    status VARCHAR(30) NOT NULL,
    execution_time_ms BIGINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS rejected_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id BIGINT,
    raw_record TEXT,
    reason VARCHAR(500) NOT NULL,
    pipeline_stage VARCHAR(50) NOT NULL,
    country VARCHAR(100) NOT NULL,
    source VARCHAR(100) NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS hs_raw (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id BIGINT NOT NULL,
    customs_territory VARCHAR(50) NOT NULL,
    country VARCHAR(100) NOT NULL,
    raw_national_code VARCHAR(100),
    raw_description TEXT,
    unit VARCHAR(50),
    source_name VARCHAR(150),
    dataset_version VARCHAR(50),
    extracted_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS hs_validated (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id BIGINT NOT NULL,
    customs_territory VARCHAR(50) NOT NULL,
    country VARCHAR(100) NOT NULL,
    chapter VARCHAR(2) NOT NULL,
    heading VARCHAR(4) NOT NULL,
    hs6 VARCHAR(6) NOT NULL,
    national_code VARCHAR(50) NOT NULL,
    code_length INT NOT NULL,
    nomenclature_type VARCHAR(50) NOT NULL,
    category VARCHAR(100) NOT NULL,
    official_description TEXT NOT NULL,
    unit VARCHAR(50),
    source_name VARCHAR(150),
    dataset_version VARCHAR(50),
    validated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS hs_master (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customs_territory VARCHAR(50) NOT NULL,
    country VARCHAR(100) NOT NULL,
    chapter VARCHAR(2) NOT NULL,
    heading VARCHAR(4) NOT NULL,
    hs6 VARCHAR(6) NOT NULL,
    national_code VARCHAR(50) NOT NULL,
    code_length INT NOT NULL,
    nomenclature_type VARCHAR(50) NOT NULL,
    category VARCHAR(100) NOT NULL,
    official_description TEXT NOT NULL,
    unit VARCHAR(50),
    dataset_version VARCHAR(50) NOT NULL,
    is_current BOOLEAN DEFAULT TRUE,
    is_active BOOLEAN DEFAULT TRUE,
    record_hash VARCHAR(64),
    remarks VARCHAR(255),
    effective_from DATETIME,
    effective_to DATETIME,
    source_id BIGINT,
    last_verified DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_customs_code_version UNIQUE (country, customs_territory, national_code, dataset_version)
);

CREATE TABLE IF NOT EXISTS hs_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hs_master_id BIGINT NOT NULL,
    customs_territory VARCHAR(50) NOT NULL,
    country VARCHAR(100) NOT NULL,
    national_code VARCHAR(50) NOT NULL,
    official_description TEXT NOT NULL,
    category VARCHAR(100) NOT NULL,
    version VARCHAR(50) NOT NULL,
    effective_from DATETIME NOT NULL,
    effective_to DATETIME,
    last_verified DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- SEED CATEGORY MASTER & NORMALIZED CHAPTERS FOR TEST H2 DB
INSERT INTO category_master (id, category_name, description) VALUES
(1, 'Mineral Fuels & Petroleum', 'Petroleum, mineral oils, bituminous substances'),
(2, 'Gems & Jewellery', 'Precious stones, diamonds, pearls, jewellery'),
(3, 'Pharmaceuticals', 'Pharmaceutical products, medicaments'),
(4, 'Machinery', 'Nuclear reactors, boilers, machinery and mechanical appliances'),
(5, 'Electrical Machinery', 'Electrical machinery, sound recorders, television equipment'),
(6, 'Iron & Steel', 'Iron and steel products'),
(7, 'Organic Chemicals', 'Organic chemical compounds'),
(8, 'Vehicles & Auto Components', 'Vehicles other than railway or tramway rolling-stock'),
(9, 'Cereals', 'Wheat, rice, corn, barley, oats'),
(10, 'Apparel & Garments', 'Articles of apparel and clothing accessories'),
(11, 'Spices', 'Coffee, tea, mate and spices'),
(12, 'Leather & Leather Goods', 'Raw hides, skins, leather, travel goods'),
(13, 'Handicrafts & Carpets', 'Carpets, special woven fabrics, handicrafts'),
(14, 'Marine Products', 'Fish, crustaceans, molluscs, aquatic invertebrates'),
(15, 'Organic Products & Cosmetics', 'Essential oils, perfumery, cosmetics, toilet preparations');

INSERT INTO category_chapter (category_id, chapter) VALUES
(1, '27'),
(2, '71'),
(3, '30'),
(4, '84'),
(5, '85'),
(6, '72'), (6, '73'),
(7, '29'),
(8, '87'),
(9, '10'),
(10, '61'), (10, '62'),
(11, '09'), (11, '9'),
(12, '41'), (12, '42'),
(13, '57'), (13, '58'), (13, '63'),
(14, '03'), (14, '3'),
(15, '33'), (15, '34');

-- REGULATION INTELLIGENCE TABLES FOR H2 TEST DB
CREATE TABLE IF NOT EXISTS regulation_source (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    country VARCHAR(100) NOT NULL,
    authority VARCHAR(255) NOT NULL,
    title TEXT,
    document_type VARCHAR(100) NOT NULL,
    source_url TEXT,
    format VARCHAR(20) NOT NULL,
    status VARCHAR(30) DEFAULT 'ACTIVE',
    last_updated DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS regulation_download_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    country VARCHAR(100) NOT NULL,
    source_id BIGINT,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    download_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    file_size BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    version VARCHAR(20) DEFAULT 'v1',
    effective_date DATETIME,
    expiry_date DATETIME,
    last_verified_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 10. Regulation Raw Text Audit Table
CREATE TABLE IF NOT EXISTS regulation_raw (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    country VARCHAR(100) NOT NULL,
    authority VARCHAR(255) NOT NULL,
    title TEXT,
    document_type VARCHAR(100),
    section TEXT,
    subsection TEXT,
    page_number INT,
    raw_text TEXT NOT NULL,
    source_url TEXT,
    download_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 11. Regulation Master Table
CREATE TABLE IF NOT EXISTS regulation_master (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    country VARCHAR(100) NOT NULL,
    authority VARCHAR(255),
    title TEXT,
    regulation_type VARCHAR(100),
    summary TEXT,
    effective_date DATETIME,
    expiry_date DATETIME,
    source_url TEXT,
    confidence_score DOUBLE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 12. Regulation Documents Table
CREATE TABLE IF NOT EXISTS regulation_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    regulation_id BIGINT NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    mandatory BOOLEAN DEFAULT TRUE,
    remarks TEXT,
    FOREIGN KEY (regulation_id) REFERENCES regulation_master(id) ON DELETE CASCADE
);

-- 13. Regulation Certifications Table
CREATE TABLE IF NOT EXISTS regulation_certifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    regulation_id BIGINT NOT NULL,
    certification_name VARCHAR(255) NOT NULL,
    mandatory BOOLEAN DEFAULT TRUE,
    remarks TEXT,
    FOREIGN KEY (regulation_id) REFERENCES regulation_master(id) ON DELETE CASCADE
);

-- 14. Regulation Labeling Table
CREATE TABLE IF NOT EXISTS regulation_labeling (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    regulation_id BIGINT NOT NULL,
    requirement TEXT NOT NULL,
    remarks TEXT,
    FOREIGN KEY (regulation_id) REFERENCES regulation_master(id) ON DELETE CASCADE
);

-- 15. Regulation Restrictions Table
CREATE TABLE IF NOT EXISTS regulation_restrictions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    regulation_id BIGINT NOT NULL,
    restriction_type VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    remarks TEXT,
    FOREIGN KEY (regulation_id) REFERENCES regulation_master(id) ON DELETE CASCADE
);

-- 16. Regulation Procedures Table
CREATE TABLE IF NOT EXISTS regulation_procedures (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    regulation_id BIGINT NOT NULL,
    procedure_name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    step_order INT,
    FOREIGN KEY (regulation_id) REFERENCES regulation_master(id) ON DELETE CASCADE
);

-- 17. Regulation HS Mapping Table
CREATE TABLE IF NOT EXISTS regulation_hs_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    regulation_id BIGINT NOT NULL,
    chapter VARCHAR(50),
    heading VARCHAR(255),
    hs6 VARCHAR(50),
    national_code VARCHAR(100),
    confidence DOUBLE,
    mapping_method VARCHAR(50),
    confidence_score DOUBLE,
    source_reference TEXT,
    FOREIGN KEY (regulation_id) REFERENCES regulation_master(id) ON DELETE CASCADE
);

-- 18. Compliance Complexity Table
CREATE TABLE IF NOT EXISTS compliance_complexity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    country VARCHAR(100) NOT NULL,
    hs_code VARCHAR(50) NOT NULL,
    score INT NOT NULL,
    level VARCHAR(50) NOT NULL,
    reasons TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_compliance_country_hs UNIQUE (country, hs_code)
);

CREATE INDEX IF NOT EXISTS idx_reg_hs_natcode ON regulation_hs_mapping(national_code);
CREATE INDEX IF NOT EXISTS idx_reg_hs_hs6 ON regulation_hs_mapping(hs6);
CREATE INDEX IF NOT EXISTS idx_reg_hs_heading ON regulation_hs_mapping(heading);
CREATE INDEX IF NOT EXISTS idx_reg_hs_chapter ON regulation_hs_mapping(chapter);
CREATE INDEX IF NOT EXISTS idx_reg_hs_regid ON regulation_hs_mapping(regulation_id);
CREATE INDEX IF NOT EXISTS idx_reg_master_country ON regulation_master(country);
CREATE INDEX IF NOT EXISTS idx_reg_dl_country_src ON regulation_download_history(country, source_id);
CREATE INDEX IF NOT EXISTS idx_hs_master_country_natcode ON hs_master(country, national_code);
CREATE INDEX IF NOT EXISTS idx_hs_master_country_hs6 ON hs_master(country, hs6);

-- 19. AUDIT TABLES (TASK 9)
CREATE TABLE IF NOT EXISTS regulation_data_quality_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    country VARCHAR(100) NOT NULL,
    record_id BIGINT,
    entity_name VARCHAR(100) NOT NULL,
    validation_status VARCHAR(50) NOT NULL,
    validation_reason TEXT,
    confidence DOUBLE,
    audit_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS regulation_evidence_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    country VARCHAR(100) NOT NULL,
    regulation_id BIGINT NOT NULL,
    requirement_type VARCHAR(50) NOT NULL,
    requirement_id BIGINT NOT NULL,
    validation_status VARCHAR(50) NOT NULL,
    source_evidence TEXT,
    confidence DOUBLE,
    audit_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS regulation_hs_mapping_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    country VARCHAR(100) NOT NULL,
    mapping_id BIGINT NOT NULL,
    national_code VARCHAR(100),
    hs6 VARCHAR(50),
    heading VARCHAR(50),
    chapter VARCHAR(50),
    hierarchy_level VARCHAR(50) NOT NULL,
    valid_in_hs_master BOOLEAN NOT NULL,
    validation_status VARCHAR(50) NOT NULL,
    source_reference TEXT,
    audit_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 20. TRADE & MARKET INTELLIGENCE TABLES
CREATE TABLE IF NOT EXISTS trade_import_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hs_code VARCHAR(50) NOT NULL,
    hs6 VARCHAR(6) NOT NULL,
    destination_country VARCHAR(100) NOT NULL,
    trade_year INT NOT NULL,
    import_value_usd DOUBLE,
    import_quantity DOUBLE,
    quantity_unit VARCHAR(50),
    world_import_value_usd DOUBLE,
    import_growth_percent DOUBLE,
    three_year_cagr DOUBLE,
    source VARCHAR(150) NOT NULL,
    source_url TEXT,
    data_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_trade_import_key UNIQUE (hs6, destination_country, trade_year)
);

CREATE TABLE IF NOT EXISTS india_export_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hs_code VARCHAR(50) NOT NULL,
    hs6 VARCHAR(6) NOT NULL,
    destination_country VARCHAR(100) NOT NULL,
    trade_year INT NOT NULL,
    india_export_value_usd DOUBLE,
    india_export_quantity DOUBLE,
    quantity_unit VARCHAR(50),
    previous_year_export_value_usd DOUBLE,
    export_growth_percent DOUBLE,
    three_year_cagr DOUBLE,
    india_market_share_percent DOUBLE,
    india_supplier_rank INT,
    source VARCHAR(150) NOT NULL,
    source_url TEXT,
    data_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_india_export_key UNIQUE (hs6, destination_country, trade_year)
);

CREATE TABLE IF NOT EXISTS tariff_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hs_code VARCHAR(50) NOT NULL,
    hs6 VARCHAR(6) NOT NULL,
    destination_country VARCHAR(100) NOT NULL,
    trade_year INT NOT NULL,
    mfn_tariff_percent DOUBLE,
    preferential_tariff_percent DOUBLE,
    applied_tariff_percent DOUBLE,
    bound_tariff_percent DOUBLE,
    tariff_quota VARCHAR(100),
    duty_free BOOLEAN DEFAULT FALSE,
    tariff_type VARCHAR(100),
    source VARCHAR(150) NOT NULL,
    source_url TEXT,
    data_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_tariff_stat_key UNIQUE (hs6, destination_country, trade_year)
);

CREATE TABLE IF NOT EXISTS competition_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hs_code VARCHAR(50) NOT NULL,
    hs6 VARCHAR(6) NOT NULL,
    destination_country VARCHAR(100) NOT NULL,
    trade_year INT NOT NULL,
    supplier_country_count INT,
    top_supplier_country VARCHAR(100),
    top_supplier_share_percent DOUBLE,
    top5_supplier_share_percent DOUBLE,
    india_market_share_percent DOUBLE,
    india_supplier_rank INT,
    supplier_hhi DOUBLE,
    source VARCHAR(150) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_competition_stat_key UNIQUE (hs6, destination_country, trade_year)
);

CREATE TABLE IF NOT EXISTS country_economic_indicators (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    country VARCHAR(100) NOT NULL,
    trade_year INT NOT NULL,
    gdp_usd DOUBLE,
    gdp_per_capita_usd DOUBLE,
    gdp_growth_percent DOUBLE,
    population BIGINT,
    inflation_percent DOUBLE,
    exchange_rate DOUBLE,
    source VARCHAR(150) NOT NULL,
    source_url TEXT,
    data_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_economic_ind_key UNIQUE (country, trade_year)
);

CREATE TABLE IF NOT EXISTS trade_data_quality_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    country VARCHAR(100) NOT NULL,
    hs6 VARCHAR(10),
    trade_year INT,
    check_type VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    source VARCHAR(150),
    audit_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_trade_imp_hs6_cty ON trade_import_statistics(hs6, destination_country);
CREATE INDEX IF NOT EXISTS idx_india_exp_hs6_cty ON india_export_statistics(hs6, destination_country);
CREATE INDEX IF NOT EXISTS idx_tariff_hs6_cty ON tariff_statistics(hs6, destination_country);
CREATE INDEX IF NOT EXISTS idx_comp_hs6_cty ON competition_statistics(hs6, destination_country);
CREATE INDEX IF NOT EXISTS idx_econ_cty_yr ON country_economic_indicators(country, trade_year);

-- 21. COUNTRY RISK INDICATORS TABLE
CREATE TABLE IF NOT EXISTS country_risk_indicators (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    country VARCHAR(100) NOT NULL,
    trade_year INT NOT NULL,
    country_risk_score INT,
    political_risk_indicator VARCHAR(50),
    economic_risk_indicator VARCHAR(50),
    trade_risk_indicator VARCHAR(50),
    source VARCHAR(150) NOT NULL,
    source_url TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_country_risk_key UNIQUE (country, trade_year)
);

-- 22. MARKET ACCESS INDICATORS TABLE
CREATE TABLE IF NOT EXISTS market_access_indicators (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    country VARCHAR(100) NOT NULL,
    trade_year INT NOT NULL,
    market_access_score INT,
    pta_indicator BOOLEAN DEFAULT FALSE,
    india_fta_indicator BOOLEAN DEFAULT FALSE,
    agreement_name VARCHAR(255),
    source VARCHAR(150) NOT NULL,
    source_url TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_market_access_key UNIQUE (country, trade_year)
);

-- 23. CURRENCY INDICATORS TABLE
CREATE TABLE IF NOT EXISTS currency_indicators (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    country VARCHAR(100) NOT NULL,
    trade_year INT NOT NULL,
    currency_code VARCHAR(10) NOT NULL,
    exchange_rate_usd DOUBLE,
    currency_volatility_percent DOUBLE,
    source VARCHAR(150) NOT NULL,
    source_url TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_currency_ind_key UNIQUE (country, trade_year)
);

-- 24. TRADE FEATURE SOURCES ATTRIBUTION TABLE
CREATE TABLE IF NOT EXISTS trade_feature_sources (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    feature_name VARCHAR(100) NOT NULL,
    country VARCHAR(100),
    hs6 VARCHAR(10),
    source_name VARCHAR(150) NOT NULL,
    source_url TEXT,
    retrieval_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    reference_year INT,
    methodology TEXT,
    confidence_status VARCHAR(50) DEFAULT 'DATA_AVAILABLE'
);

-- 25. RECOMMENDATION FEATURE AUDIT LOG TABLE
CREATE TABLE IF NOT EXISTS recommendation_feature_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    country VARCHAR(100) NOT NULL,
    hs6 VARCHAR(10) NOT NULL,
    trade_year INT NOT NULL,
    audit_status VARCHAR(50) NOT NULL, -- DATA_AVAILABLE, DATA_UNAVAILABLE, DATA_QUALITY_WARNING
    null_feature_count INT DEFAULT 0,
    quality_issue_details TEXT,
    audit_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 26. CBEC COUNTRY RECOMMENDATION DATASET (ML TRAINING FEATURE MATRIX TABLE)
CREATE TABLE IF NOT EXISTS cbec_country_recommendation_dataset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hs6 VARCHAR(6) NOT NULL,
    destination_country VARCHAR(100) NOT NULL,
    trade_year INT NOT NULL,

    -- Trade Demand Features
    import_value_usd DOUBLE,
    import_quantity DOUBLE,
    import_growth_rate DOUBLE,
    import_cagr DOUBLE,
    market_size_score INT,

    -- India Performance Features
    india_export_value_usd DOUBLE,
    india_export_quantity DOUBLE,
    india_market_share_percent DOUBLE,
    india_export_growth_rate DOUBLE,
    india_export_cagr DOUBLE,
    india_historical_export_trend VARCHAR(50),

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

    -- Compliance & Regulatory Complexity Features (from existing DB)
    compliance_score INT,
    document_count INT,
    certification_count INT,
    restriction_count INT,
    labeling_requirement_count INT,
    procedure_count INT,
    regulatory_burden_score INT,

    -- Economic Features
    gdp_usd DOUBLE,
    gdp_per_capita_usd DOUBLE,
    population BIGINT,
    gdp_growth DOUBLE,

    -- Risk Features
    country_risk_score INT,
    political_economic_risk_indicator VARCHAR(50),
    trade_risk_indicator VARCHAR(50),

    -- Accessibility Features
    market_access_score INT,
    preferential_trade_agreement_indicator BOOLEAN,
    india_trade_agreement_indicator BOOLEAN,

    -- Currency Features
    exchange_rate DOUBLE,
    exchange_rate_volatility DOUBLE,

    -- Final Derived Normalized Sub-Scores (0-100)
    demand_score INT,
    growth_score INT,
    tariff_score INT,
    compliance_score_normalized INT,
    economic_score INT,
    risk_score INT,
    accessibility_score INT,
    india_potential_score INT,

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

CREATE INDEX IF NOT EXISTS idx_cbec_ds_hs6_cty ON cbec_country_recommendation_dataset(hs6, destination_country);

-- 27. HS REGULATORY COVERAGE AUDIT TABLE (H2 Test Version)
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

CREATE INDEX IF NOT EXISTS idx_hs_reg_cov_country ON hs_regulatory_coverage_audit(country);
CREATE INDEX IF NOT EXISTS idx_hs_reg_cov_hs6 ON hs_regulatory_coverage_audit(hs6);
CREATE INDEX IF NOT EXISTS idx_hs_reg_cov_status ON hs_regulatory_coverage_audit(coverage_status);
CREATE INDEX IF NOT EXISTS idx_hs_reg_cov_cty_status ON hs_regulatory_coverage_audit(country, coverage_status);

CREATE TABLE IF NOT EXISTS regulation_hs_evidence_verification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    country VARCHAR(100) NOT NULL,
    hs_code VARCHAR(50) NOT NULL,
    hs2 VARCHAR(2),
    hs4 VARCHAR(4),
    hs6 VARCHAR(6),
    regulation_id BIGINT,
    mapping_method VARCHAR(50),
    source_id BIGINT,
    source_url TEXT,
    source_reference TEXT,
    evidence_text TEXT,
    evidence_location TEXT,
    evidence_level VARCHAR(50),
    verification_status VARCHAR(50),
    verification_reason TEXT,
    verified_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_reg_hs_evid_verif UNIQUE (country, hs_code)
);

CREATE INDEX IF NOT EXISTS idx_evid_verif_cty ON regulation_hs_evidence_verification(country);
CREATE INDEX IF NOT EXISTS idx_evid_verif_status ON regulation_hs_evidence_verification(verification_status);


