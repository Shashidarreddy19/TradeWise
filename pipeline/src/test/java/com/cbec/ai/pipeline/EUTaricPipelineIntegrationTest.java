package com.cbec.ai.pipeline;

import com.cbec.ai.pipeline.service.EUTaricEtlProcessorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EUTaricPipelineIntegrationTest {

    @Autowired
    private EUTaricEtlProcessorService euTaricEtlProcessorService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testEuTaricPipelineExecutionForGermanyAndNetherlands() {
        // Step 1: Execute for Germany
        EUTaricEtlProcessorService.EuEtlExecutionSummary germanySummary =
                euTaricEtlProcessorService.processEuPipeline("Germany", null, "EU_TARIC_2026");

        assertNotNull(germanySummary);
        assertEquals("COMPLETED", germanySummary.getStatus());
        assertEquals("Germany", germanySummary.getCountry());
        assertTrue(germanySummary.getTotalRawExtracted() > 0);
        assertTrue(germanySummary.getTotalMasterLoaded() > 0);

        // Step 2: Execute for Netherlands (reusing same extractor engine)
        EUTaricEtlProcessorService.EuEtlExecutionSummary netherlandsSummary =
                euTaricEtlProcessorService.processEuPipeline("Netherlands", null, "EU_TARIC_2026");

        assertNotNull(netherlandsSummary);
        assertEquals("COMPLETED", netherlandsSummary.getStatus());
        assertEquals("Netherlands", netherlandsSummary.getCountry());
        assertTrue(netherlandsSummary.getTotalRawExtracted() > 0);
        assertTrue(netherlandsSummary.getTotalMasterLoaded() > 0);

        // Step 3: Database Verification Queries
        System.out.println("================================================================================");
        System.out.println("EUROPEAN UNION TARIC ETL PIPELINE VERIFICATION REPORT (GERMANY & NETHERLANDS)");
        System.out.println("================================================================================");

        Long germanyRaw = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_raw WHERE country = 'Germany'", Long.class);
        Long germanyValidated = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_validated WHERE country = 'Germany'", Long.class);
        Long germanyMaster = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_master WHERE country = 'Germany'", Long.class);

        Long netherlandsRaw = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_raw WHERE country = 'Netherlands'", Long.class);
        Long netherlandsValidated = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_validated WHERE country = 'Netherlands'", Long.class);
        Long netherlandsMaster = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_master WHERE country = 'Netherlands'", Long.class);

        Long nullDescriptions = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_master WHERE customs_territory = 'EU' AND (official_description IS NULL OR official_description = '')", Long.class);
        Long invalidHierarchy = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_master WHERE customs_territory = 'EU' AND (SUBSTRING(national_code, 1, 2) != chapter OR SUBSTRING(national_code, 1, 4) != heading OR SUBSTRING(national_code, 1, 6) != hs6)", Long.class);

        System.out.println("--- GERMANY DATABASE METRICS ---");
        System.out.println("1. Germany hs_raw: " + germanyRaw);
        System.out.println("2. Germany hs_validated: " + germanyValidated);
        System.out.println("3. Germany hs_master (Supported Export Chapters): " + germanyMaster);

        System.out.println("\n--- NETHERLANDS DATABASE METRICS ---");
        System.out.println("1. Netherlands hs_raw: " + netherlandsRaw);
        System.out.println("2. Netherlands hs_validated: " + netherlandsValidated);
        System.out.println("3. Netherlands hs_master (Supported Export Chapters): " + netherlandsMaster);

        System.out.println("\n--- SHARED EU INTEGRITY METRICS ---");
        System.out.println("7. Null descriptions in EU hs_master: " + nullDescriptions);
        System.out.println("8. Invalid hierarchy rows in EU hs_master: " + invalidHierarchy);

        assertEquals(0L, nullDescriptions);
        assertEquals(0L, invalidHierarchy);

        System.out.println("\n--- RECORDS PER CATEGORY IN GERMANY hs_master ---");
        List<Map<String, Object>> gerCatRows = jdbcTemplate.queryForList("SELECT category, COUNT(*) AS total FROM hs_master WHERE country = 'Germany' GROUP BY category ORDER BY total DESC");
        for (Map<String, Object> r : gerCatRows) {
            System.out.printf("%-35s | %d%n", r.get("category"), ((Number) r.get("total")).longValue());
        }

        System.out.println("\n--- RECORDS PER CATEGORY IN NETHERLANDS hs_master ---");
        List<Map<String, Object>> nlCatRows = jdbcTemplate.queryForList("SELECT category, COUNT(*) AS total FROM hs_master WHERE country = 'Netherlands' GROUP BY category ORDER BY total DESC");
        for (Map<String, Object> r : nlCatRows) {
            System.out.printf("%-35s | %d%n", r.get("category"), ((Number) r.get("total")).longValue());
        }

        System.out.println("================================================================================");
    }
}
