package com.cbec.ai.pipeline;

import com.cbec.ai.pipeline.service.USHTSEtlProcessorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class USHTSExtractorTest {

    @Autowired
    private USHTSEtlProcessorService usHtsEtlProcessorService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testCompleteUsHtsEtlPipelineExecution() {
        // Execute US HTS Pipeline
        USHTSEtlProcessorService.UsEtlExecutionSummary summary = usHtsEtlProcessorService.processUsPipeline(null, "US_HTS_2026");

        assertNotNull(summary);
        assertEquals("COMPLETED", summary.getStatus());
        assertTrue(summary.getTotalRawExtracted() > 0);
        assertTrue(summary.getTotalValidatedInserted() > 0);
        assertTrue(summary.getTotalMasterLoaded() > 0);

        // Run Verification Queries
        System.out.println("================================================================================");
        System.out.println("UNITED STATES HTS ETL PIPELINE VERIFICATION REPORT");
        System.out.println("================================================================================");

        Long totalUsRaw = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_raw WHERE country = 'United States'", Long.class);
        Long totalUsValidated = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_validated WHERE country = 'United States'", Long.class);
        Long totalUsMaster = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_master WHERE country = 'United States'", Long.class);
        Long nullDescriptions = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_master WHERE country = 'United States' AND (official_description IS NULL OR official_description = '')", Long.class);
        Long invalidHierarchy = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_master WHERE country = 'United States' AND (SUBSTRING(national_code, 1, 2) != chapter OR SUBSTRING(national_code, 1, 4) != heading OR SUBSTRING(national_code, 1, 6) != hs6)", Long.class);

        System.out.println("1. Total US hs_raw: " + totalUsRaw);
        System.out.println("2. Total US hs_validated: " + totalUsValidated);
        System.out.println("3. Total US hs_master (Supported Export Chapters): " + totalUsMaster);
        System.out.println("4. Null descriptions in US hs_master: " + nullDescriptions);
        System.out.println("5. Invalid hierarchy rows in US hs_master: " + invalidHierarchy);

        assertEquals(0L, nullDescriptions);
        assertEquals(0L, invalidHierarchy);

        System.out.println("\n--- RECORDS PER CATEGORY IN US hs_master ---");
        List<Map<String, Object>> catRows = jdbcTemplate.queryForList("SELECT category, COUNT(*) AS total FROM hs_master WHERE country = 'United States' GROUP BY category ORDER BY total DESC");
        for (Map<String, Object> r : catRows) {
            System.out.printf("%-35s | %d%n", r.get("category"), ((Number) r.get("total")).longValue());
        }

        System.out.println("================================================================================");
    }
}
