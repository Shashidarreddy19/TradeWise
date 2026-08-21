package com.cbec.ai.pipeline;

import com.cbec.ai.pipeline.service.SouthKoreaPipelineProcessorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SouthKoreaPipelineIntegrationTest {

    @Autowired
    private SouthKoreaPipelineProcessorService southKoreaPipelineProcessorService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testSouthKoreaCustomsTariffExcelPipelineExecution() {
        SouthKoreaPipelineProcessorService.SouthKoreaEtlExecutionReport report =
                southKoreaPipelineProcessorService.processSouthKoreaPipeline(
                        "관세청_HS부호_20260101.xlsx",
                        "관세청_품목번호별 관세율표_20260211.xlsx",
                        "HSK_2026");

        assertNotNull(report);
        assertEquals("COMPLETED", report.getStatus());
        assertEquals("South Korea", report.getCountry());
        assertTrue(report.getClassificationRowsRead() > 0);
        assertTrue(report.getTariffRowsRead() > 0);
        assertTrue(report.getMergedRowsExtracted() > 0);
        assertTrue(report.getRecordsLoadedHsMaster() > 0);

        System.out.println("================================================================================");
        System.out.println("SOUTH KOREA CUSTOMS TARIFF EXCEL ETL PIPELINE VERIFICATION REPORT");
        System.out.println("================================================================================");
        System.out.println("Country: " + report.getCountry());
        System.out.println("Classification Excel: " + report.getClassificationFileName());
        System.out.println("Tariff Excel: " + report.getTariffFileName());
        System.out.println("Classification Rows Read: " + report.getClassificationRowsRead());
        System.out.println("Tariff Rows Streamed: " + report.getTariffRowsRead());
        System.out.println("Merged Records Extracted: " + report.getMergedRowsExtracted());
        System.out.println("Rows Inserted into hs_raw: " + report.getRecordsInsertedHsRaw());
        System.out.println("Rows Validated: " + report.getRecordsValidated());
        System.out.println("Rows Rejected: " + report.getRecordsRejected());
        System.out.println("Rows Category Mapped: " + report.getRecordsCategoryMapped());
        System.out.println("Rows Loaded into hs_master: " + report.getRecordsLoadedHsMaster());
        System.out.println("Rows Loaded into hs_versions: " + report.getRecordsLoadedHsVersions());
        System.out.println("Execution Time: " + report.getExecutionTimeMs() + " ms");

        // Verification SQL Queries
        Long totalKrRaw = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hs_raw WHERE country='South Korea'", Long.class);
        Long totalKrValidated = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hs_validated WHERE country='South Korea'", Long.class);
        Long totalKrMaster = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hs_master WHERE country='South Korea'", Long.class);
        Long nullNationalCodes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hs_master WHERE country='South Korea' AND national_code IS NULL", Long.class);
        Long nullDescriptions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hs_master WHERE country='South Korea' AND (official_description IS NULL OR official_description = '')", Long.class);
        Long invalidHierarchy = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hs_master WHERE country='South Korea' AND (SUBSTRING(national_code, 1, 2) != chapter OR SUBSTRING(national_code, 1, 4) != heading OR SUBSTRING(national_code, 1, 6) != hs6)", Long.class);

        System.out.println("\n--- DATABASE VERIFICATION METRICS ---");
        System.out.println("1. Total South Korea hs_raw: " + totalKrRaw);
        System.out.println("2. Total South Korea hs_validated: " + totalKrValidated);
        System.out.println("3. Total South Korea hs_master (Supported 21 Chapters): " + totalKrMaster);
        System.out.println("5. Null national_code count in South Korea hs_master: " + nullNationalCodes);
        System.out.println("7. Null descriptions in South Korea hs_master: " + nullDescriptions);
        System.out.println("8. Invalid hierarchy rows in South Korea hs_master: " + invalidHierarchy);

        assertEquals(0L, nullNationalCodes);
        assertEquals(0L, nullDescriptions);
        assertEquals(0L, invalidHierarchy);

        List<Map<String, Object>> duplicateCheck = jdbcTemplate.queryForList(
                "SELECT national_code, COUNT(*) AS total FROM hs_master WHERE country='South Korea' GROUP BY national_code HAVING COUNT(*) > 1");
        System.out.println("6. Duplicate national_code count in South Korea hs_master: " + duplicateCheck.size());
        assertEquals(0, duplicateCheck.size());

        System.out.println("\n--- RECORDS PER CHAPTER IN SOUTH KOREA hs_master ---");
        List<Map<String, Object>> chRows = jdbcTemplate.queryForList(
                "SELECT chapter, COUNT(*) AS total FROM hs_master WHERE country='South Korea' GROUP BY chapter ORDER BY chapter");
        for (Map<String, Object> r : chRows) {
            System.out.printf("Chapter %-5s | %d%n", r.get("chapter"), ((Number) r.get("total")).longValue());
        }

        System.out.println("================================================================================");
    }
}
