package com.cbec.ai.pipeline.intelligence.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class CountryComparisonService {

    private final ExportGuideService exportGuideService;
    private final ComplianceComplexityService complexityService;

    public CountryComparisonService(
            ExportGuideService exportGuideService,
            ComplianceComplexityService complexityService) {
        this.exportGuideService = exportGuideService;
        this.complexityService = complexityService;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CountryComparisonItemDto {
        private String country;
        private String hsCode;
        private Map<String, String> tariff;
        private int documentCount;
        private int certificationCount;
        private int restrictionCount;
        private int labelingCount;
        private int procedureCount;
        private ComplianceComplexityService.ComplexityReportDto complianceComplexity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CountryComparisonResponseDto {
        private String hsCode;
        private List<CountryComparisonItemDto> comparison;
    }

    public CountryComparisonResponseDto compareCountries(String hsCode, List<String> countries) {
        log.info("Comparing countries {} for HS Code: {}", countries, hsCode);

        List<CountryComparisonItemDto> items = new ArrayList<>();
        for (String c : countries) {
            String trimmed = c.trim();
            if (trimmed.isEmpty()) continue;

            ExportGuideService.ExportGuideDto guide = exportGuideService.getExportGuide(trimmed, hsCode);
            ComplianceComplexityService.ComplexityReportDto complexity = complexityService.getComplexity(trimmed, hsCode);

            items.add(CountryComparisonItemDto.builder()
                    .country(trimmed)
                    .hsCode(hsCode)
                    .tariff(guide.getTariff())
                    .documentCount(guide.getDocuments().size())
                    .certificationCount(guide.getCertifications().size())
                    .restrictionCount(guide.getRestrictions().size())
                    .labelingCount(guide.getLabeling().size())
                    .procedureCount(guide.getProcedures().size())
                    .complianceComplexity(complexity)
                    .build());
        }

        return CountryComparisonResponseDto.builder()
                .hsCode(hsCode)
                .comparison(items)
                .build();
    }
}
