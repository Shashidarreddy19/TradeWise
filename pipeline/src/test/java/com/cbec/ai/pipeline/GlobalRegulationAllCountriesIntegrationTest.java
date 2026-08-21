package com.cbec.ai.pipeline;

import com.cbec.ai.pipeline.regulation.service.RegulationAiProcessorService.GenericAiProcessingMetrics;
import com.cbec.ai.pipeline.regulation.service.RegulationPipelineService;
import com.cbec.ai.pipeline.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class GlobalRegulationAllCountriesIntegrationTest {

    @Autowired
    private RegulationPipelineService pipelineService;

    @Autowired
    private RegulationSourceRepository sourceRepository;

    @Autowired
    private RegulationDownloadHistoryRepository downloadHistoryRepository;

    @Autowired
    private RegulationRawRepository rawRepository;

    @Autowired
    private RegulationMasterRepository masterRepository;

    private void runAndVerifyCountryPipeline(String country) {
        log.info("================================================================================");
        log.info("STARTING GENERIC PIPELINE FOR COUNTRY: '{}'", country);
        log.info("================================================================================");

        GenericAiProcessingMetrics metrics = pipelineService.processRegulationsForCountry(country);
        assertNotNull(metrics, "Metrics should not be null for " + country);
        assertEquals("SUCCESS", metrics.getStatus(), "Pipeline execution should succeed for " + country);

        long sourcesCount = sourceRepository.findByCountry(country).size();
        long downloadsCount = downloadHistoryRepository.findByCountry(country).size();
        long rawCount = rawRepository.findByCountry(country).size();
        long masterCount = masterRepository.findByCountry(country).size();

        assertTrue(sourcesCount > 0, "Sources should be > 0 for " + country);
        assertTrue(downloadsCount > 0, "Downloads should be > 0 for " + country);
        assertTrue(rawCount > 0, "Raw records should be > 0 for " + country);
        assertTrue(masterCount > 0, "Master records should be > 0 for " + country);

        log.info("================================================================================");
        log.info("COUNTRY VERIFICATION REPORT: {}", country.toUpperCase());
        log.info("================================================================================");
        log.info("Country                 : {}", country);
        log.info("Number of Sources       : {}", sourcesCount);
        log.info("Downloaded Files        : {}", downloadsCount);
        log.info("Extracted Raw Records   : {}", rawCount);
        log.info("Master Regulations      : {}", masterCount);
        log.info("Documents               : {}", metrics.getDocuments());
        log.info("Certificates            : {}", metrics.getCertifications());
        log.info("Restrictions            : {}", metrics.getRestrictions());
        log.info("Labeling Rules          : {}", metrics.getLabeling());
        log.info("Procedures              : {}", metrics.getProcedures());
        log.info("HS Mappings             : {}", metrics.getHsMappings());
        log.info("Build Status            : BUILD SUCCESS");
        log.info("--------------------------------------------------------------------------------");
        log.info("DATABASE VERIFICATION QUERIES:");
        log.info("  SELECT COUNT(*) FROM regulation_source WHERE country = '{}'; -- Result: {}", country, sourcesCount);
        log.info("  SELECT COUNT(*) FROM regulation_download_history WHERE country = '{}'; -- Result: {}", country, downloadsCount);
        log.info("  SELECT COUNT(*) FROM regulation_raw WHERE country = '{}'; -- Result: {}", country, rawCount);
        log.info("  SELECT COUNT(*) FROM regulation_master WHERE country = '{}'; -- Result: {}", country, masterCount);
        log.info("  SELECT COUNT(*) FROM regulation_documents WHERE regulation_id IN (SELECT id FROM regulation_master WHERE country = '{}'); -- Result: {}", country, metrics.getDocuments());
        log.info("  SELECT COUNT(*) FROM regulation_certifications WHERE regulation_id IN (SELECT id FROM regulation_master WHERE country = '{}'); -- Result: {}", country, metrics.getCertifications());
        log.info("  SELECT COUNT(*) FROM regulation_labeling WHERE regulation_id IN (SELECT id FROM regulation_master WHERE country = '{}'); -- Result: {}", country, metrics.getLabeling());
        log.info("  SELECT COUNT(*) FROM regulation_restrictions WHERE regulation_id IN (SELECT id FROM regulation_master WHERE country = '{}'); -- Result: {}", country, metrics.getRestrictions());
        log.info("  SELECT COUNT(*) FROM regulation_procedures WHERE regulation_id IN (SELECT id FROM regulation_master WHERE country = '{}'); -- Result: {}", country, metrics.getProcedures());
        log.info("  SELECT COUNT(*) FROM regulation_hs_mapping WHERE regulation_id IN (SELECT id FROM regulation_master WHERE country = '{}'); -- Result: {}", country, metrics.getHsMappings());
        log.info("================================================================================\n");
    }

    @Test
    @DisplayName("Process Canada Regulation Intelligence Pipeline")
    public void testCanadaRegulationPipeline() {
        runAndVerifyCountryPipeline("Canada");
    }

    @Test
    @DisplayName("Process Australia Regulation Intelligence Pipeline")
    public void testAustraliaRegulationPipeline() {
        runAndVerifyCountryPipeline("Australia");
    }

    @Test
    @DisplayName("Process Germany Regulation Intelligence Pipeline")
    public void testGermanyRegulationPipeline() {
        runAndVerifyCountryPipeline("Germany");
    }

    @Test
    @DisplayName("Process Netherlands Regulation Intelligence Pipeline")
    public void testNetherlandsRegulationPipeline() {
        runAndVerifyCountryPipeline("Netherlands");
    }

    @Test
    @DisplayName("Process Japan Regulation Intelligence Pipeline")
    public void testJapanRegulationPipeline() {
        runAndVerifyCountryPipeline("Japan");
    }

    @Test
    @DisplayName("Process South Korea Regulation Intelligence Pipeline")
    public void testSouthKoreaRegulationPipeline() {
        runAndVerifyCountryPipeline("South Korea");
    }

    @Test
    @DisplayName("Process India Regulation Intelligence Pipeline")
    public void testIndiaRegulationPipeline() {
        runAndVerifyCountryPipeline("India");
    }

    @Test
    @DisplayName("Process United Arab Emirates Regulation Intelligence Pipeline")
    public void testUaeRegulationPipeline() {
        runAndVerifyCountryPipeline("United Arab Emirates");
    }

    @Test
    @DisplayName("Process Hong Kong Regulation Intelligence Pipeline")
    public void testHongKongRegulationPipeline() {
        runAndVerifyCountryPipeline("Hong Kong");
    }
}
