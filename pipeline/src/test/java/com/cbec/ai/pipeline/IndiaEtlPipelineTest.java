package com.cbec.ai.pipeline;

import com.cbec.ai.pipeline.service.IndiaEtlPipelineProcessorService;
import com.cbec.ai.pipeline.service.IndiaItcHsPdfExtractorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class IndiaEtlPipelineTest {

    @Autowired
    private IndiaItcHsPdfExtractorService pdfExtractorService;

    @Autowired
    private IndiaEtlPipelineProcessorService etlPipelineProcessorService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testCompleteIndiaEtlPipelineExecution() {
        // Step 1: Ensure PDF extraction has populated hs_raw
        Long rawCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_raw WHERE dataset_version = 'ITC_HS_2022'", Long.class);
        if (rawCount == null || rawCount == 0) {
            pdfExtractorService.extractItcHsPdf("ITC-HS_2022.pdf");
        }

        // Step 2: Execute India ETL Pipeline: hs_raw -> hs_validated -> hs_master
        IndiaEtlPipelineProcessorService.EtlExecutionSummary summary = etlPipelineProcessorService.processIndiaPipeline();

        assertNotNull(summary);
        assertEquals("COMPLETED", summary.getStatus());
        assertTrue(summary.getTotalRawRead() > 0);
        assertTrue(summary.getTotalValidatedInserted() > 0);
        assertTrue(summary.getTotalMasterLoaded() > 0);

        // Step 3: Run Database Verification Queries
        System.out.println("================================================================================");
        System.out.println("INDIA ETL PIPELINE VERIFICATION REPORT");
        System.out.println("================================================================================");

        Long totalHsRaw = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_raw", Long.class);
        Long totalHsValidated = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_validated", Long.class);
        Long totalHsMaster = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_master", Long.class);
        Long nullDescriptions = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_master WHERE official_description IS NULL OR official_description = ''", Long.class);

        System.out.println("1. Total hs_raw: " + totalHsRaw);
        System.out.println("2. Total hs_validated: " + totalHsValidated);
        System.out.println("3. Total hs_master (Supported 21 Chapters): " + totalHsMaster);
        System.out.println("7. Null descriptions in hs_master: " + nullDescriptions);

        // Records per category in hs_master
        System.out.println("\n--- RECORDS PER CATEGORY IN hs_master ---");
        List<Map<String, Object>> catRows = jdbcTemplate.queryForList("SELECT category, COUNT(*) AS total FROM hs_master GROUP BY category ORDER BY total DESC");
        for (Map<String, Object> r : catRows) {
            System.out.printf("%-35s | %d%n", r.get("category"), ((Number) r.get("total")).longValue());
        }

        // Records per chapter in hs_master
        System.out.println("\n--- RECORDS PER CHAPTER IN hs_master ---");
        List<Map<String, Object>> chRows = jdbcTemplate.queryForList("SELECT chapter, COUNT(*) AS total FROM hs_master GROUP BY chapter ORDER BY chapter");
        for (Map<String, Object> r : chRows) {
            System.out.printf("Chapter %-5s | %d%n", r.get("chapter"), ((Number) r.get("total")).longValue());
        }

        System.out.println("================================================================================");
    }
}
