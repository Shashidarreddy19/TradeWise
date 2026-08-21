package com.cbec.ai.pipeline.trade.service;

import com.cbec.ai.pipeline.model.entity.CbecCountryRecommendationDatasetEntity;
import com.cbec.ai.pipeline.model.entity.RecommendationFeatureAuditEntity;
import com.cbec.ai.pipeline.repository.CbecCountryRecommendationDatasetRepository;
import com.cbec.ai.pipeline.repository.RecommendationFeatureAuditRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class TradeFeatureValidationService {

    private final CbecCountryRecommendationDatasetRepository datasetRepository;
    private final RecommendationFeatureAuditRepository auditRepository;

    public TradeFeatureValidationService(
            CbecCountryRecommendationDatasetRepository datasetRepository,
            RecommendationFeatureAuditRepository auditRepository) {
        this.datasetRepository = datasetRepository;
        this.auditRepository = auditRepository;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureValidationReportDto {
        private Integer totalVectorsAudited;
        private Long totalAvailableRecords;
        private Long totalUnavailableRecords;
        private Long totalWarningRecords;
        private Double overallDatasetCompletenessPercent;
        private List<RecommendationFeatureAuditEntity> auditLogs;
    }

    @Transactional
    public FeatureValidationReportDto runDatasetQualityValidation() {
        List<CbecCountryRecommendationDatasetEntity> dataset = datasetRepository.findAll();
        long available = 0;
        long unavailable = 0;
        long warnings = 0;

        List<RecommendationFeatureAuditEntity> logs = new ArrayList<>();

        for (CbecCountryRecommendationDatasetEntity row : dataset) {
            int nullCount = 0;
            StringBuilder issues = new StringBuilder();

            if (row.getImportValueUsd() == null) nullCount++;
            if (row.getIndiaExportValueUsd() == null) nullCount++;
            if (row.getMfnTariffPercent() == null) nullCount++;
            if (row.getSupplierHhi() == null) nullCount++;
            if (row.getGdpUsd() == null) nullCount++;

            if (row.getImportValueUsd() != null && row.getImportValueUsd() < 0) {
                issues.append("Negative importValueUsd; ");
            }
            if (row.getMfnTariffPercent() != null && (row.getMfnTariffPercent() < 0 || row.getMfnTariffPercent() > 100)) {
                issues.append("Invalid MFN tariff range; ");
            }

            String status = "DATA_AVAILABLE";
            if (nullCount > 2) {
                status = "DATA_UNAVAILABLE";
                unavailable++;
            } else if (!issues.isEmpty()) {
                status = "DATA_QUALITY_WARNING";
                warnings++;
            } else {
                available++;
            }

            RecommendationFeatureAuditEntity auditLog = RecommendationFeatureAuditEntity.builder()
                    .country(row.getDestinationCountry())
                    .hs6(row.getHs6())
                    .year(row.getYear())
                    .auditStatus(status)
                    .nullFeatureCount(nullCount)
                    .qualityIssueDetails(issues.isEmpty() ? "100% Valid Feature Vector" : issues.toString())
                    .build();

            auditRepository.save(auditLog);
            logs.add(auditLog);
        }

        double completeness = dataset.isEmpty() ? 100.0 : Math.round(((double) available / dataset.size() * 100.0) * 100.0) / 100.0;

        return FeatureValidationReportDto.builder()
                .totalVectorsAudited(dataset.size())
                .totalAvailableRecords(available)
                .totalUnavailableRecords(unavailable)
                .totalWarningRecords(warnings)
                .overallDatasetCompletenessPercent(completeness)
                .auditLogs(logs)
                .build();
    }
}
