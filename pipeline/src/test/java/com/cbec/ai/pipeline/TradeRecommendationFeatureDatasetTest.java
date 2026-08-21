package com.cbec.ai.pipeline;

import com.cbec.ai.pipeline.model.entity.CbecCountryRecommendationDatasetEntity;
import com.cbec.ai.pipeline.repository.CbecCountryRecommendationDatasetRepository;
import com.cbec.ai.pipeline.trade.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TradeRecommendationFeatureDatasetTest {

    @Autowired
    private TradeDataIngestionService ingestionService;

    @Autowired
    private TradeFeatureIngestionService featureIngestionService;

    @Autowired
    private TradeFeatureEngineeringService featureEngineeringService;

    @Autowired
    private TradeFeatureValidationService featureValidationService;

    @Autowired
    private CbecCountryRecommendationDatasetRepository datasetRepository;

    @Test
    @DisplayName("Test 1: Complete Trade Recommendation Dataset Generation & Feature Engineering (HS6 x Country x Year)")
    public void testCompleteTradeFeatureDatasetPipeline() {
        ingestionService.executeTradePipelineIngestion();
        featureIngestionService.ingestAllAuxiliaryFeatureData();

        long persistedRows = featureEngineeringService.generateAndPersistRecommendationDataset();
        assertTrue(persistedRows > 0, "Persisted dataset rows count must be > 0");

        TradeFeatureValidationService.FeatureValidationReportDto auditReport = featureValidationService.runDatasetQualityValidation();
        assertNotNull(auditReport);
        assertTrue(auditReport.getTotalVectorsAudited() > 0, "Audited feature vectors count must be > 0");
        assertTrue(auditReport.getOverallDatasetCompletenessPercent() >= 80.0, "Dataset completeness must exceed 80.0%");

        List<CbecCountryRecommendationDatasetEntity> allRows = datasetRepository.findAll();
        assertFalse(allRows.isEmpty());

        allRows.forEach(row -> {
            assertNotNull(row.getHs6());
            assertNotNull(row.getDestinationCountry());
            assertNotNull(row.getYear());
            assertNotNull(row.getImportValueUsd());
            assertNotNull(row.getIndiaExportValueUsd());
            assertNotNull(row.getMfnTariffPercent());
            assertNotNull(row.getSupplierHhi());
            assertNotNull(row.getComplianceScore(), "Compliance score must be derived");
            assertNotNull(row.getDocumentCount(), "Document count from existing DB must be present");
            assertNotNull(row.getCertificationCount(), "Certification count from existing DB must be present");
            assertNotNull(row.getRestrictionCount(), "Restriction count from existing DB must be present");
            assertNotNull(row.getGdpUsd());
            assertNotNull(row.getCountryRiskScore());
            assertNotNull(row.getMarketAccessScore());
            assertNotNull(row.getExchangeRate());
            assertTrue(row.getIndiaPotentialScore() >= 0 && row.getIndiaPotentialScore() <= 100);
            assertNotNull(row.getTradeSource());
            assertNotNull(row.getTariffSource());
            assertNotNull(row.getEconomicSource());
        });
    }
}
