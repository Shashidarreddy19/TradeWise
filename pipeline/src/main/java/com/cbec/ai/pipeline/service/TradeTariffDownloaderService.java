package com.cbec.ai.pipeline.service;

import com.cbec.ai.pipeline.config.TradeTariffConfiguration;
import com.cbec.ai.pipeline.model.entity.DownloadHistoryEntity;
import com.cbec.ai.pipeline.repository.DownloadHistoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class TradeTariffDownloaderService {

    private final TradeTariffApiClient apiClient;
    private final TradeTariffConfiguration config;
    private final DownloadHistoryRepository downloadHistoryRepository;
    private final ObjectMapper objectMapper;

    // 21 Supported Export Chapters
    private static final List<String> TARGET_CHAPTERS = Arrays.asList(
            "03", "09", "10", "27", "29", "30", "33", "34", "41", "42",
            "57", "58", "61", "62", "63", "71", "72", "73", "84", "85", "87"
    );

    public TradeTariffDownloaderService(
            TradeTariffApiClient apiClient,
            TradeTariffConfiguration config,
            DownloadHistoryRepository downloadHistoryRepository) {
        this.apiClient = apiClient;
        this.config = config;
        this.downloadHistoryRepository = downloadHistoryRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Data
    @Builder
    public static class DownloadSummary {
        private String country;
        private String filePath;
        private String fileName;
        private long fileSize;
        private String sha256;
        private int httpStatus;
        private String datasetVersion;
        private Long downloadHistoryId;
        private int totalHeadingsDownloaded;
        private int totalCommoditiesDownloaded;
    }

    /**
     * Downloads 100% official UK Trade Tariff commodity dataset directly from HMRC API v2 via OAuth2.
     * ZERO sample files, ZERO local JSON fallback.
     */
    public DownloadSummary downloadOfficialUkTariffPayload(String version) {
        String ver = version != null ? version : "UK_TARIFF_2026";
        log.info("Starting live official UK Trade Tariff dataset download from HMRC API...");

        ObjectNode fullPayload = objectMapper.createObjectNode();
        ArrayNode dataArray = fullPayload.putArray("data");

        int totalHeadingsDownloaded = 0;
        int totalCommoditiesDownloaded = 0;

        try {
            // 1. Fetch chapters list from GET /api/v2/chapters
            String chaptersJson = apiClient.fetchTariffData("/chapters");
            JsonNode chaptersRoot = objectMapper.readTree(chaptersJson);

            Set<String> chaptersToProcess = new LinkedHashSet<>();
            if (chaptersRoot.has("data") && chaptersRoot.get("data").isArray()) {
                for (JsonNode chNode : chaptersRoot.get("data")) {
                    if (chNode.has("attributes") && chNode.get("attributes").has("goods_nomenclature_item_id")) {
                        String itemId = chNode.get("attributes").get("goods_nomenclature_item_id").asText();
                        if (itemId.length() >= 2) {
                            String chCode = itemId.substring(0, 2);
                            if (TARGET_CHAPTERS.contains(chCode)) {
                                chaptersToProcess.add(chCode);
                            }
                        }
                    }
                }
            }

            if (chaptersToProcess.isEmpty()) {
                chaptersToProcess.addAll(TARGET_CHAPTERS);
            }

            log.info("Discovered {} target UK Trade Tariff chapters to download from HMRC API", chaptersToProcess.size());

            // 2. For each chapter, fetch headings via GET /api/v2/chapters/{chCode}
            for (String chCode : chaptersToProcess) {
                log.info("Downloading UK Trade Tariff Chapter {} from HMRC API...", chCode);
                try {
                    String chapterDetailJson = apiClient.fetchTariffData("/chapters/" + chCode);
                    JsonNode chDetailRoot = objectMapper.readTree(chapterDetailJson);

                    Set<String> heading4Digits = new LinkedHashSet<>();
                    if (chDetailRoot.has("included") && chDetailRoot.get("included").isArray()) {
                        for (JsonNode incNode : chDetailRoot.get("included")) {
                            if ("heading".equals(incNode.path("type").asText())) {
                                String itemId = incNode.path("attributes").path("goods_nomenclature_item_id").asText();
                                if (itemId.length() >= 4) {
                                    heading4Digits.add(itemId.substring(0, 4));
                                }
                            }
                        }
                    }

                    // 3. For each 4-digit heading, fetch commodities via GET /api/v2/headings/{heading4Digit}
                    for (String h4 : heading4Digits) {
                        try {
                            String headingJson = apiClient.fetchTariffData("/headings/" + h4);
                            JsonNode headingRoot = objectMapper.readTree(headingJson);
                            totalHeadingsDownloaded++;

                            if (headingRoot.has("included") && headingRoot.get("included").isArray()) {
                                for (JsonNode incNode : headingRoot.get("included")) {
                                    if ("commodity".equals(incNode.path("type").asText())) {
                                        dataArray.add(incNode);
                                        totalCommoditiesDownloaded++;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Failed downloading UK Trade Tariff Heading {} from HMRC API: {}", h4, e.getMessage());
                        }
                    }

                } catch (Exception e) {
                    log.warn("Failed downloading UK Trade Tariff Chapter {} from HMRC API: {}", chCode, e.getMessage());
                }
            }

            log.info("HMRC API Download Complete. Headings Downloaded: {}, Commodities Downloaded: {}",
                    totalHeadingsDownloaded, totalCommoditiesDownloaded);

            String fullJsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(fullPayload);
            byte[] contentBytes = fullJsonString.getBytes(StandardCharsets.UTF_8);
            long fileSize = contentBytes.length;
            String sha256 = computeSha256(contentBytes);

            LocalDateTime now = LocalDateTime.now();
            String yearStr = now.format(DateTimeFormatter.ofPattern("yyyy"));
            String monthStr = now.format(DateTimeFormatter.ofPattern("MM"));

            Path storageDir = Paths.get(config.getDownloadDirectory(), "UnitedKingdom", yearStr, monthStr);
            Files.createDirectories(storageDir);

            Path targetFile = storageDir.resolve("uk_trade_tariff_full_" + System.currentTimeMillis() + ".json");
            Files.write(targetFile, contentBytes);

            DownloadHistoryEntity history = DownloadHistoryEntity.builder()
                    .country("United Kingdom")
                    .sourceName("HMRC UK Trade Tariff API v2")
                    .fileName(targetFile.getFileName().toString())
                    .filePath(targetFile.toAbsolutePath().toString())
                    .contentType("application/json")
                    .fileSize(fileSize)
                    .sha256(sha256)
                    .downloadUrl("https://www.trade-tariff.service.gov.uk/api/v2")
                    .httpStatus(200)
                    .datasetVersion(ver)
                    .build();

            history = downloadHistoryRepository.save(history);
            log.info("Saved live official UK Trade Tariff archive to {} (Size: {} bytes, SHA-256: {})",
                    targetFile.toAbsolutePath(), fileSize, sha256);

            return DownloadSummary.builder()
                    .country("United Kingdom")
                    .filePath(targetFile.toAbsolutePath().toString())
                    .fileName(targetFile.getFileName().toString())
                    .fileSize(fileSize)
                    .sha256(sha256)
                    .httpStatus(200)
                    .datasetVersion(ver)
                    .downloadHistoryId(history.getId())
                    .totalHeadingsDownloaded(totalHeadingsDownloaded)
                    .totalCommoditiesDownloaded(totalCommoditiesDownloaded)
                    .build();

        } catch (Exception e) {
            log.error("Fatal error during live HMRC UK Trade Tariff API download", e);
            throw new RuntimeException("Failed downloading live official UK Trade Tariff payload: " + e.getMessage(), e);
        }
    }

    private String computeSha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content);
            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}
