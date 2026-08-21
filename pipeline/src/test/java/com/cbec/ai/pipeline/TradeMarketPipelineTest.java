package com.cbec.ai.pipeline;

import com.cbec.ai.pipeline.trade.service.CountryRecommendationDatasetService;
import com.cbec.ai.pipeline.trade.service.TradeDataIngestionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TradeMarketPipelineTest {

    @Autowired
    private TradeDataIngestionService ingestionService;

    @Autowired
    private CountryRecommendationDatasetService datasetService;

    @Test
    @DisplayName("Test 1: Trade & Market Intelligence Pipeline Ingestion Across 11 Destination Countries")
    public void testTradePipelineIngestion() {
        TradeDataIngestionService.TradeIngestionSummaryDto summary = ingestionService.executeTradePipelineIngestion();

        assertNotNull(summary);
        assertEquals(11, summary.getCountriesProcessed(), "Must process all 11 target export markets");
        assertTrue(summary.getHsCodesProcessed() > 0, "Processed HS6 codes count must be > 0");
        assertTrue(summary.getCombinationsProcessed() > 0, "HS6 x Country x Year combinations count must be > 0");
        assertTrue(summary.getImportRecordsCount() > 0, "Import records count must be > 0");
        assertTrue(summary.getIndiaExportRecordsCount() > 0, "India export records count must be > 0");
        assertTrue(summary.getTariffRecordsCount() > 0, "Tariff records count must be > 0");
        assertTrue(summary.getCompetitionRecordsCount() > 0, "Competition records count must be > 0");
        assertTrue(summary.getEconomicRecordsCount() > 0, "Economic records count must be > 0");
        assertTrue(summary.getOverallSourceCoveragePercent() >= 80.0, "Source coverage % must exceed 80.0%");
    }

    @Test
    @DisplayName("Test 2: Country Recommendation Feature Matrix & Dataset Generation (HS6 x Country x Year)")
    public void testRecommendationDatasetGeneration() {
        ingestionService.executeTradePipelineIngestion();
        CountryRecommendationDatasetService.RecommendationDatasetMatrixDto dataset = datasetService.generateRecommendationDatasetMatrix(null);

        assertNotNull(dataset);
        assertTrue(dataset.getTotalVectorsGenerated() > 0, "Total generated feature vectors must be > 0");
        assertEquals(11, dataset.getTargetCountriesCount(), "Must cover all 11 target export markets");
        assertNotNull(dataset.getFeatureVectors());
        assertFalse(dataset.getFeatureVectors().isEmpty());

        dataset.getFeatureVectors().forEach(vec -> {
            assertNotNull(vec.getHs6());
            assertNotNull(vec.getDestinationCountry());
            assertNotNull(vec.getYear());
            assertNotNull(vec.getImportValueUsd());
            assertNotNull(vec.getMfnTariffPercent());
            assertNotNull(vec.getSupplierHhi(), "Supplier HHI must be computed");
            assertNotNull(vec.getComplianceScore(), "Compliance score must be derived");
            assertTrue(vec.getCompositeRecommendationScore() >= 0 && vec.getCompositeRecommendationScore() <= 100);
            assertNotNull(vec.getTradeSource());
            assertNotNull(vec.getTariffSource());
            assertNotNull(vec.getEconomicSource());
        });
    }
}
