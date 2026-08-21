package com.cbec.ai.pipeline.ukregulation.service;

import com.cbec.ai.pipeline.model.entity.RegulationDownloadHistoryEntity;
import com.cbec.ai.pipeline.model.entity.RegulationSourceEntity;
import com.cbec.ai.pipeline.repository.RegulationDownloadHistoryRepository;
import com.cbec.ai.pipeline.repository.RegulationSourceRepository;
import com.cbec.ai.pipeline.ukregulation.config.UkRegulationConfiguration;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
public class UkRegulationDownloaderService {

    private final RegulationSourceRepository sourceRepository;
    private final RegulationDownloadHistoryRepository downloadHistoryRepository;
    private final UkRegulationConfiguration config;

    public UkRegulationDownloaderService(
            RegulationSourceRepository sourceRepository,
            RegulationDownloadHistoryRepository downloadHistoryRepository,
            UkRegulationConfiguration config) {
        this.sourceRepository = sourceRepository;
        this.downloadHistoryRepository = downloadHistoryRepository;
        this.config = config;
    }

    @Data
    @Builder
    public static class DownloadResult {
        private RegulationSourceEntity source;
        private RegulationDownloadHistoryEntity downloadHistory;
        private Path localFilePath;
        private String contentType;
        private byte[] content;
        private boolean success;
    }

    /**
     * Downloads every ACTIVE UK regulation source document and records history into `regulation_download_history`.
     */
    public List<DownloadResult> downloadActiveUkRegulations() {
        log.info("Starting download of active United Kingdom regulation sources...");

        List<RegulationSourceEntity> sources = sourceRepository.findByCountryAndStatus("United Kingdom", "ACTIVE");
        log.info("Found {} active UK regulation sources", sources.size());

        List<DownloadResult> results = new ArrayList<>();

        for (RegulationSourceEntity source : sources) {
            DownloadResult res = downloadSingleSource(source);
            results.add(res);
        }

        return results;
    }

    public DownloadResult downloadSingleSource(RegulationSourceEntity source) {
        log.info("Downloading UK Regulation Source ID: {}, Title: '{}', URL: {}",
                source.getId(), source.getTitle(), source.getSourceUrl());

        String url = source.getSourceUrl();
        if ("https://www.trade-tariff.service.gov.uk/api/v2".equals(url)) {
            url = "https://www.trade-tariff.service.gov.uk/api/v2/sections";
        }
        byte[] downloadedBytes = null;
        String contentType = "text/html";
        int httpStatus = 0;
        Exception lastException = null;

        for (int attempt = 1; attempt <= config.getRetryCount(); attempt++) {
            try {
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    HttpClient client = HttpClient.newBuilder()
                            .followRedirects(HttpClient.Redirect.ALWAYS)
                            .connectTimeout(Duration.ofMillis(config.getTimeout()))
                            .build();

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("User-Agent", config.getUserAgent())
                            .header("Accept", "*/*")
                            .timeout(Duration.ofMillis(config.getTimeout()))
                            .GET()
                            .build();

                    HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                    httpStatus = response.statusCode();

                    if (httpStatus >= 200 && httpStatus < 300) {
                        downloadedBytes = response.body();
                        contentType = response.headers().firstValue("Content-Type").orElse("text/html");
                        break;
                    }
                }
            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {}/{} failed for URL {}: {}", attempt, config.getRetryCount(), url, e.getMessage());
                try {
                    Thread.sleep(1000L * attempt);
                } catch (InterruptedException ignored) {}
            }
        }

        if (downloadedBytes == null) {
            log.error("Failed to download UK Regulation Source ID {} after {} retries: {}",
                    source.getId(), config.getRetryCount(), lastException != null ? lastException.getMessage() : "HTTP Status " + httpStatus);

            RegulationDownloadHistoryEntity history = RegulationDownloadHistoryEntity.builder()
                    .country("United Kingdom")
                    .sourceId(source.getId())
                    .fileName("failed_download.txt")
                    .filePath("NONE")
                    .sha256("FAILED")
                    .downloadTime(LocalDateTime.now())
                    .fileSize(0L)
                    .status("FAILED")
                    .build();

            downloadHistoryRepository.save(history);

            return DownloadResult.builder()
                    .source(source)
                    .downloadHistory(history)
                    .success(false)
                    .build();
        }

        try {
            String sha256 = computeSha256(downloadedBytes);
            LocalDateTime now = LocalDateTime.now();
            String yearStr = now.format(DateTimeFormatter.ofPattern("yyyy"));
            String monthStr = now.format(DateTimeFormatter.ofPattern("MM"));

            Path storageDir = Paths.get(config.getDownloadDirectory(), yearStr, monthStr);
            Files.createDirectories(storageDir);

            String fileExt = determineFileExtension(source.getFormat(), contentType);
            String safeTitle = source.getTitle().replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
            String fileName = "uk_reg_" + safeTitle + "_" + System.currentTimeMillis() + fileExt;

            Path targetFile = storageDir.resolve(fileName);
            Files.write(targetFile, downloadedBytes);

            RegulationDownloadHistoryEntity history = RegulationDownloadHistoryEntity.builder()
                    .country("United Kingdom")
                    .sourceId(source.getId())
                    .fileName(fileName)
                    .filePath(targetFile.toAbsolutePath().toString())
                    .sha256(sha256)
                    .downloadTime(now)
                    .fileSize((long) downloadedBytes.length)
                    .status("SUCCESS")
                    .build();

            history = downloadHistoryRepository.save(history);
            source.setLastUpdated(now);
            sourceRepository.save(source);

            log.info("Successfully downloaded and archived UK Regulation ID {} to {} (Size: {} bytes, SHA-256: {})",
                    source.getId(), targetFile.toAbsolutePath(), downloadedBytes.length, sha256);

            return DownloadResult.builder()
                    .source(source)
                    .downloadHistory(history)
                    .localFilePath(targetFile)
                    .contentType(contentType)
                    .content(downloadedBytes)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Error storing downloaded UK regulation file for source ID {}", source.getId(), e);
            throw new RuntimeException("Error storing downloaded regulation file: " + e.getMessage(), e);
        }
    }

    private String determineFileExtension(String format, String contentType) {
        if ("PDF".equalsIgnoreCase(format) || (contentType != null && contentType.contains("pdf"))) {
            return ".pdf";
        }
        if ("JSON".equalsIgnoreCase(format) || "API".equalsIgnoreCase(format) || (contentType != null && contentType.contains("json"))) {
            return ".json";
        }
        if ("XML".equalsIgnoreCase(format) || (contentType != null && contentType.contains("xml"))) {
            return ".xml";
        }
        if ("ZIP".equalsIgnoreCase(format) || (contentType != null && contentType.contains("zip"))) {
            return ".zip";
        }
        return ".html";
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
