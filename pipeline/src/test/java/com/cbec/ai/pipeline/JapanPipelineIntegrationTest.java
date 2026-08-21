package com.cbec.ai.pipeline;

import com.cbec.ai.pipeline.service.JapanPipelineProcessorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JapanPipelineIntegrationTest {

    @Autowired
    private JapanPipelineProcessorService japanPipelineProcessorService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testJapanCustomsTariffWebCrawlingPipelineExecution() {
        JapanPipelineProcessorService.JapanEtlExecutionReport report =
                japanPipelineProcessorService.processJapanPipeline("JAPAN_TARIFF_2026");

        assertNotNull(report);
        assertEquals("COMPLETED", report.getStatus());
        assertEquals("Japan", report.getCountry());
        assertTrue(report.getSectionsDiscovered() > 0);
        assertTrue(report.getChaptersDiscovered() > 0);
        assertTrue(report.getPagesDownloaded() > 0);
        assertTrue(report.getRecordsExtracted() > 0);
        assertTrue(report.getRecordsLoaded() > 0);

        System.out.println("================================================================================");
        System.out.println("JAPAN CUSTOMS TARIFF WEB CRAWLING ETL PIPELINE VERIFICATION REPORT");
        System.out.println("================================================================================");
        System.out.println("Country: " + report.getCountry());
        System.out.println("Sections Discovered: " + report.getSectionsDiscovered());
        System.out.println("Chapters Discovered: " + report.getChaptersDiscovered());
        System.out.println("Pages Downloaded: " + report.getPagesDownloaded());
        System.out.println("Records Extracted: " + report.getRecordsExtracted());
        System.out.println("Records Validated: " + report.getRecordsValidated());
        System.out.println("Records Loaded into hs_master: " + report.getRecordsLoaded());
        System.out.println("Execution Time: " + report.getExecutionTimeMs() + " ms");

        // Verification SQL Queries
        Long totalJpRaw = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hs_raw WHERE country='Japan'", Long.class);
        Long totalJpValidated = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hs_validated WHERE country='Japan'", Long.class);
        Long totalJpMaster = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hs_master WHERE country='Japan'", Long.class);
        Long nullNationalCodes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hs_master WHERE country='Japan' AND national_code IS NULL", Long.class);
        Long nullDescriptions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hs_master WHERE country='Japan' AND (official_description IS NULL OR official_description = '')", Long.class);
        Long invalidHierarchy = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hs_master WHERE country='Japan' AND (SUBSTRING(national_code, 1, 2) != chapter OR SUBSTRING(national_code, 1, 4) != heading OR SUBSTRING(national_code, 1, 6) != hs6)", Long.class);

        System.out.println("\n--- DATABASE VERIFICATION METRICS ---");
        System.out.println("1. Total Japan hs_raw: " + totalJpRaw);
        System.out.println("2. Total Japan hs_validated: " + totalJpValidated);
        System.out.println("3. Total Japan hs_master (Supported 21 Chapters): " + totalJpMaster);
        System.out.println("5. Null national_code count in Japan hs_master: " + nullNationalCodes);
        System.out.println("7. Null descriptions in Japan hs_master: " + nullDescriptions);
        System.out.println("8. Invalid hierarchy rows in Japan hs_master: " + invalidHierarchy);

        assertEquals(0L, nullNationalCodes);
        assertEquals(0L, nullDescriptions);
        assertEquals(0L, invalidHierarchy);

        List<Map<String, Object>> duplicateCheck = jdbcTemplate.queryForList(
                "SELECT national_code, COUNT(*) AS total FROM hs_master WHERE country='Japan' GROUP BY national_code HAVING COUNT(*) > 1");
        System.out.println("6. Duplicate national_code count in Japan hs_master: " + duplicateCheck.size());
        assertEquals(0, duplicateCheck.size());

        System.out.println("\n--- RECORDS PER CHAPTER IN JAPAN hs_master ---");
        List<Map<String, Object>> chRows = jdbcTemplate.queryForList(
                "SELECT chapter, COUNT(*) AS total FROM hs_master WHERE country='Japan' GROUP BY chapter ORDER BY chapter");
        for (Map<String, Object> r : chRows) {
            System.out.printf("Chapter %-5s | %d%n", r.get("chapter"), ((Number) r.get("total")).longValue());
        }

        System.out.println("================================================================================");
    }
}
