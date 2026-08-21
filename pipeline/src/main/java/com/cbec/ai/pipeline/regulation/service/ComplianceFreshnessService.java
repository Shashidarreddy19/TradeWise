package com.cbec.ai.pipeline.regulation.service;

import com.cbec.ai.pipeline.model.entity.RegulationDownloadHistoryEntity;
import com.cbec.ai.pipeline.model.entity.RegulationSourceEntity;
import com.cbec.ai.pipeline.repository.RegulationDownloadHistoryRepository;
import com.cbec.ai.pipeline.repository.RegulationSourceRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ComplianceFreshnessService {

    private final RegulationSourceRepository sourceRepository;
    private final RegulationDownloadHistoryRepository downloadHistoryRepository;

    public ComplianceFreshnessService(
            RegulationSourceRepository sourceRepository,
            RegulationDownloadHistoryRepository downloadHistoryRepository) {
        this.sourceRepository = sourceRepository;
        this.downloadHistoryRepository = downloadHistoryRepository;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceFreshnessItemDto {
        private Long sourceId;
        private String country;
        private String authority;
        private String sourceTitle;
        private String sourceStatus;
        private String currentVersion;
        private String previousVersion;
        private String sha256;
        private String changedStatus; // UPDATED, UNCHANGED, INITIAL_INGESTION
        private LocalDateTime lastVerifiedDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FreshnessReportResponseDto {
        private Integer totalSourcesMonitored;
        private Integer updatedSourcesCount;
        private Integer unchangedSourcesCount;
        private List<SourceFreshnessItemDto> sources;
    }

    public FreshnessReportResponseDto getRegulationFreshnessReport(String countryFilter) {
        List<RegulationSourceEntity> sources = (countryFilter != null && !countryFilter.isBlank())
                ? sourceRepository.findByCountry(countryFilter)
                : sourceRepository.findAll();

        log.info("Generating regulation freshness & SHA-256 audit report for {} source(s)", sources.size());

        List<SourceFreshnessItemDto> items = new ArrayList<>();
        int updatedCount = 0;
        int unchangedCount = 0;

        for (RegulationSourceEntity src : sources) {
            List<RegulationDownloadHistoryEntity> histories = downloadHistoryRepository.findByCountry(src.getCountry());

            // Filter history records matching this specific source ID
            List<RegulationDownloadHistoryEntity> srcHistories = histories.stream()
                    .filter(h -> src.getId().equals(h.getSourceId()))
                    .sorted((h1, h2) -> h2.getDownloadTime().compareTo(h1.getDownloadTime()))
                    .toList();

            String currentVer = "v1";
            String previousVer = "N/A";
            String sha256 = "N/A";
            String changedStatus = "UNCHANGED";
            LocalDateTime lastVerified = src.getLastUpdated() != null ? src.getLastUpdated() : LocalDateTime.now();

            if (!srcHistories.isEmpty()) {
                RegulationDownloadHistoryEntity latest = srcHistories.get(0);
                currentVer = latest.getVersion() != null ? latest.getVersion() : "v1";
                sha256 = latest.getSha256();
                lastVerified = latest.getLastVerifiedAt() != null ? latest.getLastVerifiedAt() : latest.getDownloadTime();
                changedStatus = "UNCHANGED".equalsIgnoreCase(latest.getStatus()) ? "UNCHANGED" : "UPDATED";

                if (srcHistories.size() > 1) {
                    previousVer = srcHistories.get(1).getVersion() != null ? srcHistories.get(1).getVersion() : "v1";
                    if (!latest.getSha256().equalsIgnoreCase(srcHistories.get(1).getSha256())) {
                        changedStatus = "UPDATED";
                    }
                }
            }

            if ("UPDATED".equalsIgnoreCase(changedStatus)) {
                updatedCount++;
            } else {
                unchangedCount++;
            }

            items.add(SourceFreshnessItemDto.builder()
                    .sourceId(src.getId())
                    .country(src.getCountry())
                    .authority(src.getAuthority())
                    .sourceTitle(src.getTitle())
                    .sourceStatus(src.getStatus() != null ? src.getStatus() : "ACTIVE")
                    .currentVersion(currentVer)
                    .previousVersion(previousVer)
                    .sha256(sha256)
                    .changedStatus(changedStatus)
                    .lastVerifiedDate(lastVerified)
                    .build());
        }

        return FreshnessReportResponseDto.builder()
                .totalSourcesMonitored(sources.size())
                .updatedSourcesCount(updatedCount)
                .unchangedSourcesCount(unchangedCount)
                .sources(items)
                .build();
    }
}
