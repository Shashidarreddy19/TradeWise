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
public class GenericRegulationPipelineIntegrationTest {

    @Autowired
    private RegulationPipelineService genericPipelineService;

    @Autowired
    private RegulationSourceRepository sourceRepository;

    @Autowired
    private RegulationDownloadHistoryRepository downloadHistoryRepository;

    @Autowired
    private RegulationRawRepository rawRepository;

    @Autowired
    private RegulationMasterRepository masterRepository;

    @Test
    @DisplayName("Verify Backward Compatibility for United Kingdom using Generic Regulation Framework")
    public void testUkGenericPipelineExecution() {
        log.info("Starting Generic Regulation Framework Test for United Kingdom...");
        GenericAiProcessingMetrics metrics = genericPipelineService.processRegulationsForCountry("United Kingdom");

        assertNotNull(metrics, "Metrics should not be null for United Kingdom");
        assertEquals("SUCCESS", metrics.getStatus());
        assertEquals("United Kingdom", metrics.getCountry());

        long sourcesCount = sourceRepository.findByCountry("United Kingdom").size();
        long downloadsCount = downloadHistoryRepository.findByCountry("United Kingdom").size();
        long rawCount = rawRepository.findByCountry("United Kingdom").size();
        long masterCount = masterRepository.findByCountry("United Kingdom").size();

        assertTrue(sourcesCount > 0, "UK sources count should be > 0");
        assertTrue(downloadsCount > 0, "UK downloads count should be > 0");
        assertTrue(rawCount > 0, "UK raw count should be > 0");
        assertTrue(masterCount > 0, "UK master count should be > 0");

        log.info("UNITED KINGDOM GENERIC PIPELINE VERIFICATION SUCCESS!");
    }

    @Test
    @DisplayName("Verify Backward Compatibility for United States using Generic Regulation Framework")
    public void testUsGenericPipelineExecution() {
        log.info("Starting Generic Regulation Framework Test for United States...");
        GenericAiProcessingMetrics metrics = genericPipelineService.processRegulationsForCountry("United States");

        assertNotNull(metrics, "Metrics should not be null for United States");
        assertEquals("SUCCESS", metrics.getStatus());
        assertEquals("United States", metrics.getCountry());

        long sourcesCount = sourceRepository.findByCountry("United States").size();
        long downloadsCount = downloadHistoryRepository.findByCountry("United States").size();
        long rawCount = rawRepository.findByCountry("United States").size();
        long masterCount = masterRepository.findByCountry("United States").size();

        assertTrue(sourcesCount >= 20, "US sources count should be >= 20");
        assertTrue(downloadsCount > 0, "US downloads count should be > 0");
        assertTrue(rawCount > 0, "US raw count should be > 0");
        assertTrue(masterCount > 0, "US master count should be > 0");

        log.info("UNITED STATES GENERIC PIPELINE VERIFICATION SUCCESS!");
    }
}
