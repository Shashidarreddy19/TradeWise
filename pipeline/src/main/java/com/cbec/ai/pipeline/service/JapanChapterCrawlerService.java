package com.cbec.ai.pipeline.service;

import com.cbec.ai.pipeline.model.entity.DownloadHistoryEntity;
import com.cbec.ai.pipeline.repository.DownloadHistoryRepository;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

@Slf4j
@Service
public class JapanChapterCrawlerService {

    private final DownloadHistoryRepository downloadHistoryRepository;

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final int TIMEOUT_MS = 60000;
    private static final int MAX_RETRIES = 3;

    public JapanChapterCrawlerService(DownloadHistoryRepository downloadHistoryRepository) {
        this.downloadHistoryRepository = downloadHistoryRepository;
    }

    @Data
    @Builder
    public static class JapanChapterCrawlResult {
        private String chapterNumber;
        private String chapterUrl;
        private String localFilePath;
        private long fileSize;
        private String sha256;
        private int httpStatus;
        private Document document;
        private boolean success;
    }

    /**
     * Crawls a single Japan Customs Tariff chapter URL with retry logic, 60s timeout, and HTML archiving into `downloads/Japan/yyyy/MM/`.
     */
    public JapanChapterCrawlResult crawlJapanChapterPage(JapanScheduleCrawlerService.DiscoveredChapter chapter, String datasetVersion) {
        String version = datasetVersion != null ? datasetVersion : "JAPAN_TARIFF_2026";
        String url = chapter.getChapterUrl();
        String chNum = chapter.getChapterNumber();

        log.info("Crawling Japan Customs Tariff Chapter {} URL: {}", chNum, url);

        Document doc = null;
        int status = 0;
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Connection.Response response = Jsoup.connect(url)
                        .userAgent(USER_AGENT)
                        .timeout(TIMEOUT_MS)
                        .maxBodySize(0)
                        .execute();

                status = response.statusCode();
                if (status == 200) {
                    doc = response.parse();
                    break;
                }
            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {}/{} failed for Japan Chapter {} URL {}: {}", attempt, MAX_RETRIES, chNum, url, e.getMessage());
                try {
                    Thread.sleep(1000L * attempt);
                } catch (InterruptedException ignored) {}
            }
        }

        if (doc == null) {
            log.error("Failed to crawl Japan Chapter {} after {} attempts: {}", chNum, MAX_RETRIES, url, lastException);
            return JapanChapterCrawlResult.builder()
                    .chapterNumber(chNum)
                    .chapterUrl(url)
                    .httpStatus(status != 0 ? status : 500)
                    .success(false)
                    .build();
        }

        try {
            String htmlContent = doc.outerHtml();
            byte[] bytes = htmlContent.getBytes(StandardCharsets.UTF_8);
            String sha256 = computeSha256(bytes);

            LocalDateTime now = LocalDateTime.now();
            String yearStr = now.format(DateTimeFormatter.ofPattern("yyyy"));
            String monthStr = now.format(DateTimeFormatter.ofPattern("MM"));

            Path storageDir = Paths.get("downloads", "Japan", yearStr, monthStr);
            Files.createDirectories(storageDir);

            Path targetFile = storageDir.resolve("chapter_" + chNum + "_" + System.currentTimeMillis() + ".html");
            Files.write(targetFile, bytes);

            DownloadHistoryEntity history = DownloadHistoryEntity.builder()
                    .country("Japan")
                    .sourceName("Japan Customs Tariff")
                    .fileName(targetFile.getFileName().toString())
                    .filePath(targetFile.toAbsolutePath().toString())
                    .contentType("text/html")
                    .fileSize((long) bytes.length)
                    .sha256(sha256)
                    .downloadUrl(url)
                    .httpStatus(status)
                    .datasetVersion(version)
                    .build();

            downloadHistoryRepository.save(history);

            return JapanChapterCrawlResult.builder()
                    .chapterNumber(chNum)
                    .chapterUrl(url)
                    .localFilePath(targetFile.toAbsolutePath().toString())
                    .fileSize(bytes.length)
                    .sha256(sha256)
                    .httpStatus(status)
                    .document(doc)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Error archiving HTML for Japan Chapter {}", chNum, e);
            return JapanChapterCrawlResult.builder()
                    .chapterNumber(chNum)
                    .chapterUrl(url)
                    .document(doc)
                    .httpStatus(status)
                    .success(true)
                    .build();
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
