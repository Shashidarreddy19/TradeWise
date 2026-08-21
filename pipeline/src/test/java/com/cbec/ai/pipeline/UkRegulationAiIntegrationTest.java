package com.cbec.ai.pipeline;

import com.cbec.ai.pipeline.ukregulation.service.UkRegulationAiPipelineService;
import com.cbec.ai.pipeline.ukregulation.service.UkRegulationAiProcessorService;
import com.cbec.ai.pipeline.ukregulation.service.UkRegulationPipelineProcessorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UkRegulationAiIntegrationTest {

    @Autowired
    private UkRegulationPipelineProcessorService ukRegulationPipelineProcessorService;

    @Autowired
    private UkRegulationAiPipelineService ukRegulationAiPipelineService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testUkRegulationAiPipelineExecution() {
        // Step 1: Execute Phase 1 Pipeline to populate regulation_raw
        ukRegulationPipelineProcessorService.processUkRegulations();

        Long rawCountBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM regulation_raw WHERE country='United Kingdom'", Long.class);
        assertTrue(rawCountBefore > 0, "regulation_raw must contain records from Phase 1");

        // Step 2: Execute Phase 2 AI Regulation Intelligence Pipeline
        UkRegulationAiProcessorService.AiProcessingMetrics metrics =
                ukRegulationAiPipelineService.processUkRegulationsAiPipeline();

        assertNotNull(metrics);
        assertEquals("SUCCESS", metrics.getStatus());
        assertEquals("United Kingdom", metrics.getCountry());
        assertEquals(rawCountBefore, metrics.getProcessed());
        assertTrue(metrics.getMasterRecords() > 0, "Master records must be generated");
        assertTrue(metrics.getDocuments() > 0, "Document compliance records must be populated");
        assertTrue(metrics.getCertifications() > 0, "Certification compliance records must be populated");
        assertTrue(metrics.getRestrictions() > 0, "Restriction compliance records must be populated");
        assertTrue(metrics.getLabeling() > 0, "Labeling requirement records must be populated");
        assertTrue(metrics.getProcedures() > 0, "Procedure step records must be populated");

        System.out.println("================================================================================");
        System.out.println("PHASE 2: AI REGULATION INTELLIGENCE STRUCTURING VERIFICATION REPORT");
        System.out.println("================================================================================");
        System.out.println("Country: " + metrics.getCountry());
        System.out.println("Total Raw Records Processed: " + metrics.getProcessed());
        System.out.println("Master Regulation Records Created: " + metrics.getMasterRecords());
        System.out.println("Required Document Records Inserted: " + metrics.getDocuments());
        System.out.println("Mandatory Certification Records Inserted: " + metrics.getCertifications());
        System.out.println("Import Restriction Records Inserted: " + metrics.getRestrictions());
        System.out.println("Labeling Requirement Records Inserted: " + metrics.getLabeling());
        System.out.println("Procedure Step Records Inserted: " + metrics.getProcedures());
        System.out.println("HS Code Mapping Records Inserted: " + metrics.getHsMappings());

        // SQL Verification Queries directly against H2 / MySQL DB
        Long sqlMaster = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM regulation_master WHERE country='United Kingdom'", Long.class);
        Long sqlDocs = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM regulation_documents", Long.class);
        Long sqlCerts = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM regulation_certifications", Long.class);
        Long sqlLabels = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM regulation_labeling", Long.class);
        Long sqlRestr = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM regulation_restrictions", Long.class);
        Long sqlProcs = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM regulation_procedures", Long.class);
        Long sqlHs = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM regulation_hs_mapping", Long.class);
        Long sqlConfidenceValid = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM regulation_master WHERE confidence_score > 0.0", Long.class);

        System.out.println("\n--- DATABASE VERIFICATION METRICS ---");
        System.out.println("1. regulation_master total records: " + sqlMaster);
        System.out.println("2. regulation_documents total records: " + sqlDocs);
        System.out.println("3. regulation_certifications total records: " + sqlCerts);
        System.out.println("4. regulation_labeling total records: " + sqlLabels);
        System.out.println("5. regulation_restrictions total records: " + sqlRestr);
        System.out.println("6. regulation_procedures total records: " + sqlProcs);
        System.out.println("7. regulation_hs_mapping total records: " + sqlHs);
        System.out.println("8. Valid Confidence Scores (> 0.0): " + sqlConfidenceValid);

        assertEquals(sqlMaster, metrics.getMasterRecords());
        assertTrue(sqlMaster > 0);
        assertTrue(sqlConfidenceValid > 0);

        // Step 3: Test Idempotency (Executing Phase 2 again must preserve master count without duplicates)
        UkRegulationAiProcessorService.AiProcessingMetrics metrics2 =
                ukRegulationAiPipelineService.processUkRegulationsAiPipeline();

        Long sqlMasterAfter = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM regulation_master WHERE country='United Kingdom'", Long.class);
        assertEquals(sqlMaster, sqlMasterAfter, "Master count must remain identical on idempotent re-execution");
        System.out.println("9. Idempotent Execution Verified (Master count after re-run: " + sqlMasterAfter + ")");
        System.out.println("================================================================================");
    }
}
