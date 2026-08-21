package com.cbec.ai.pipeline;

import com.cbec.ai.pipeline.service.USHTSCsvPipelineProcessorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class USHTSCsvPipelineTest {

    @Autowired
    private USHTSCsvPipelineProcessorService usHtsCsvPipelineProcessorService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testUsHtsCsvFullEtlPipelineExecution() {
        USHTSCsvPipelineProcessorService.UsCsvEtlSummary summary =
                usHtsCsvPipelineProcessorService.processUsHtsCsv("hts_2026_basic_edition_csv.csv", "HTS_2026");

        assertNotNull(summary);
        assertEquals("COMPLETED", summary.getStatus());
        assertTrue(summary.getTotalRowsRead() > 0);
        assertTrue(summary.getFinalTariffRecordsExtracted() > 0);
        assertTrue(summary.getMasterLoaded() > 0);

        System.out.println("================================================================================");
        System.out.println("USITC HTS 2026 BASIC EDITION CSV ETL PIPELINE VERIFICATION REPORT");
        System.out.println("================================================================================");
        System.out.println("Rows Read: " + summary.getTotalRowsRead());
        System.out.println("Heading Rows Ignored: " + summary.getHeadingRowsIgnored());
        System.out.println("Group Rows Processed: " + summary.getGroupRowsProcessed());
        System.out.println("Intermediate Rows Processed: " + summary.getIntermediateRowsProcessed());
        System.out.println("Final Tariff Records Extracted: " + summary.getFinalTariffRecordsExtracted());
        System.out.println("Execution Time: " + summary.getExecutionTimeMs() + " ms");

        Long totalUsRaw = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_raw WHERE country = 'United States'", Long.class);
        Long totalUsValidated = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_validated WHERE country = 'United States'", Long.class);
        Long totalUsMaster = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_master WHERE country = 'United States'", Long.class);
        Long nullDescriptions = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_master WHERE country = 'United States' AND (official_description IS NULL OR official_description = '')", Long.class);
        Long invalidHierarchy = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_master WHERE country = 'United States' AND (SUBSTRING(national_code, 1, 2) != chapter OR SUBSTRING(national_code, 1, 4) != heading OR SUBSTRING(national_code, 1, 6) != hs6)", Long.class);

        System.out.println("\n--- DATABASE VERIFICATION METRICS ---");
        System.out.println("1. Total US hs_raw: " + totalUsRaw);
        System.out.println("2. Total US hs_validated: " + totalUsValidated);
        System.out.println("3. Total US hs_master (Supported 21 Chapters): " + totalUsMaster);
        System.out.println("7. Null descriptions in US hs_master: " + nullDescriptions);
        System.out.println("8. Invalid hierarchy rows in US hs_master: " + invalidHierarchy);

        assertEquals(0L, nullDescriptions);
        assertEquals(0L, invalidHierarchy);

        System.out.println("\n--- RECORDS PER CATEGORY IN US hs_master ---");
        List<Map<String, Object>> catRows = jdbcTemplate.queryForList("SELECT category, COUNT(*) AS total FROM hs_master WHERE country = 'United States' GROUP BY category ORDER BY total DESC");
        for (Map<String, Object> r : catRows) {
            System.out.printf("%-35s | %d%n", r.get("category"), ((Number) r.get("total")).longValue());
        }

        System.out.println("\n--- RECORDS PER CHAPTER IN US hs_master ---");
        List<Map<String, Object>> chRows = jdbcTemplate.queryForList("SELECT chapter, COUNT(*) AS total FROM hs_master WHERE country = 'United States' GROUP BY chapter ORDER BY chapter");
        for (Map<String, Object> r : chRows) {
            System.out.printf("Chapter %-5s | %d%n", r.get("chapter"), ((Number) r.get("total")).longValue());
        }

        System.out.println("================================================================================");
    }
}
