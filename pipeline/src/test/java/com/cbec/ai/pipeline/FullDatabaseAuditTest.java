package com.cbec.ai.pipeline;

import com.cbec.ai.pipeline.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

@SpringBootTest
class FullDatabaseAuditTest {

    @Autowired
    private IndiaItcHsPdfExtractorService indiaPdfExtractorService;

    @Autowired
    private IndiaEtlPipelineProcessorService indiaPipeline;

    @Autowired
    private USHTSCsvPipelineProcessorService usPipeline;

    @Autowired
    private EuCnRdfPipelineProcessorService euPipeline;

    @Autowired
    private UkTradeTariffEtlProcessorService ukPipeline;

    @Autowired
    private UaeGccPipelineProcessorService uaePipeline;

    @Autowired
    private HongKongPipelineProcessorService hkPipeline;

    @Autowired
    private AustraliaPipelineProcessorService auPipeline;

    @Autowired
    private CanadaPipelineProcessorService caPipeline;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void runFullAuditReport() {
        // Execute all 9 country pipelines in fresh memory DB
        indiaPdfExtractorService.extractItcHsPdf("ITC-HS_2022.pdf");
        indiaPipeline.processIndiaPipeline();

        usPipeline.processUsHtsCsv("hts_2026_basic_edition_csv.csv", "HTS_2026");

        euPipeline.processEuCnRdfPipeline("Germany", "ESTAT-CN2026.rdf", "CN_2026");
        euPipeline.processEuCnRdfPipeline("Netherlands", "ESTAT-CN2026.rdf", "CN_2026");

        ukPipeline.processUkPipeline("UK_TARIFF_2026");
        uaePipeline.processUaePipeline("HSCodeMaster-v3.3customers.xlsx", "GCC_TARIFF_v3.3");
        hkPipeline.processHongKongPipeline("B2XX00232026XXXXB0100 (1).csv", "HKHS_2026");
        auPipeline.processAustraliaPipeline("AU_TARIFF_2026");
        caPipeline.processCanadaPipeline("01-99-2026-eng.pdf", "CANADA_TARIFF_2026");

        System.out.println("================================================================================");
        System.out.println("FULL CBEC-AI GLOBAL TRADE TARIFF DATABASE AUDIT REPORT");
        System.out.println("================================================================================");

        Long totalCountries = jdbcTemplate.queryForObject("SELECT COUNT(DISTINCT country) FROM hs_master", Long.class);
        Long totalMasterHsCodes = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_master", Long.class);
        Long totalRawHsCodes = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_raw", Long.class);
        Long totalValidatedHsCodes = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_validated", Long.class);
        Long totalVersions = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_versions", Long.class);

        System.out.println("Total Covered Countries in Database: " + totalCountries);
        System.out.println("Total Extracted Raw HS Records (hs_raw): " + totalRawHsCodes);
        System.out.println("Total Validated HS Records (hs_validated): " + totalValidatedHsCodes);
        System.out.println("Total Master HS Tariff Lines (hs_master): " + totalMasterHsCodes);
        System.out.println("Total Historical Version Entries (hs_versions): " + totalVersions);

        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("COUNTRY-BY-COUNTRY HS CODE BREAKDOWN (hs_master vs hs_raw)");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("%-22s | %-16s | %-12s | %-12s | %-14s%n",
                "Country", "Customs Territory", "Raw Records", "Validated", "Master Loaded");
        System.out.println("--------------------------------------------------------------------------------");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT country, customs_territory, " +
                "(SELECT COUNT(*) FROM hs_raw r WHERE r.country = m.country) AS raw_cnt, " +
                "(SELECT COUNT(*) FROM hs_validated v WHERE v.country = m.country) AS val_cnt, " +
                "COUNT(*) AS master_cnt " +
                "FROM hs_master m " +
                "GROUP BY country, customs_territory " +
                "ORDER BY country");

        for (Map<String, Object> r : rows) {
            System.out.printf("%-22s | %-16s | %-12d | %-12d | %-14d%n",
                    r.get("country"),
                    r.get("customs_territory"),
                    ((Number) r.get("raw_cnt")).longValue(),
                    ((Number) r.get("val_cnt")).longValue(),
                    ((Number) r.get("master_cnt")).longValue());
        }

        System.out.println("================================================================================");
    }
}
