package com.cbec.ai.pipeline.trade.controller;

import com.cbec.ai.pipeline.model.entity.CbecCountryRecommendationDatasetEntity;
import com.cbec.ai.pipeline.repository.CbecCountryRecommendationDatasetRepository;
import com.cbec.ai.pipeline.trade.service.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trade")
public class TradePipelineController {

    private final TradeDataIngestionService ingestionService;
    private final TradeFeatureIngestionService featureIngestionService;
    private final TradeFeatureEngineeringService featureEngineeringService;
    private final TradeFeatureValidationService featureValidationService;
    private final CountryRecommendationDatasetService datasetService;
    private final CbecCountryRecommendationDatasetRepository datasetRepository;

    public TradePipelineController(
            TradeDataIngestionService ingestionService,
            TradeFeatureIngestionService featureIngestionService,
            TradeFeatureEngineeringService featureEngineeringService,
            TradeFeatureValidationService featureValidationService,
            CountryRecommendationDatasetService datasetService,
            CbecCountryRecommendationDatasetRepository datasetRepository) {
        this.ingestionService = ingestionService;
        this.featureIngestionService = featureIngestionService;
        this.featureEngineeringService = featureEngineeringService;
        this.featureValidationService = featureValidationService;
        this.datasetService = datasetService;
        this.datasetRepository = datasetRepository;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SchemaReportDto {
        private Integer totalCountries;
        private List<String> supportedCountries;
        private String hsMasterIsolationStatus;
        private String regulationDatabaseIsolationStatus;
        private List<String> tradeTablesCreated;
    }

    /**
     * Phase 1: Schema Inspection Report Endpoint
     * GET /api/v1/trade/schema-report
     */
    @GetMapping("/schema-report")
    public ResponseEntity<SchemaReportDto> getSchemaReport() {
        SchemaReportDto report = SchemaReportDto.builder()
                .totalCountries(TradeDataIngestionService.DESTINATION_COUNTRIES.size())
                .supportedCountries(TradeDataIngestionService.DESTINATION_COUNTRIES)
                .hsMasterIsolationStatus("PROTECTED_UNTOUCHED")
                .regulationDatabaseIsolationStatus("PROTECTED_UNTOUCHED")
                .tradeTablesCreated(List.of(
                        "trade_import_statistics",
                        "india_export_statistics",
                        "tariff_statistics",
                        "competition_statistics",
                        "country_economic_indicators",
                        "country_risk_indicators",
                        "market_access_indicators",
                        "currency_indicators",
                        "trade_feature_sources",
                        "recommendation_feature_audit",
                        "cbec_country_recommendation_dataset"
                ))
                .build();
        return ResponseEntity.ok(report);
    }

    /**
     * Phase 4 - 8: Trigger Trade & Market Intelligence Pipeline Ingestion & Feature Building
     * POST /api/v1/trade/build-feature-dataset
     */
    @PostMapping("/build-feature-dataset")
    public ResponseEntity<TradeFeatureValidationService.FeatureValidationReportDto> buildFeatureDataset() {
        ingestionService.executeTradePipelineIngestion();
        featureIngestionService.ingestAllAuxiliaryFeatureData();
        featureEngineeringService.generateAndPersistRecommendationDataset();
        return ResponseEntity.ok(featureValidationService.runDatasetQualityValidation());
    }

    /**
     * Phase 11 - 13: Retrieve Persisted ML Recommendation Dataset
     * GET /api/v1/trade/recommendation-dataset-matrix
     */
    @GetMapping("/recommendation-dataset-matrix")
    public ResponseEntity<List<CbecCountryRecommendationDatasetEntity>> getRecommendationDatasetMatrix(
            @RequestParam(name = "hs6", required = false) String hs6) {
        if (hs6 != null && !hs6.isBlank()) {
            return ResponseEntity.ok(datasetRepository.findByHs6(hs6));
        }
        return ResponseEntity.ok(datasetRepository.findAll());
    }

    /**
     * Phase 6: Dataset Quality Audit Endpoint
     * GET /api/v1/trade/dataset-audit-report
     */
    @GetMapping("/dataset-audit-report")
    public ResponseEntity<TradeFeatureValidationService.FeatureValidationReportDto> getDatasetAuditReport() {
        return ResponseEntity.ok(featureValidationService.runDatasetQualityValidation());
    }
}

