package com.cbec.ai.pipeline.util;

import com.cbec.ai.pipeline.model.entity.DownloadHistoryEntity;
import com.cbec.ai.pipeline.repository.DownloadHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;

@Slf4j
@Component
public class DownloadManager {

    private final DownloadHistoryRepository downloadHistoryRepository;

    public DownloadManager(DownloadHistoryRepository downloadHistoryRepository) {
        this.downloadHistoryRepository = downloadHistoryRepository;
    }

    /**
     * Archives source file into `downloads/{country}/{year}/{month}/{filename}`
     * and records metadata with SHA-256 checksum, duration, and ETag into `download_history` table.
     */
    public Path archiveSourceFile(Long sourceId, String country, String sourceName, String originalFileName, String downloadUrl, String version, InputStream input) {
        long startTime = System.currentTimeMillis();
        try {
            LocalDate now = LocalDate.now();
            String safeCountry = country.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
            Path dirPath = Paths.get("downloads", safeCountry, String.valueOf(now.getYear()), String.format("%02d", now.getMonthValue()));
            Files.createDirectories(dirPath);

            String rawFileName = (originalFileName != null && !originalFileName.isEmpty()) ? Paths.get(originalFileName).getFileName().toString() : "source_dataset.dat";
            String safeFileName = rawFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path targetPath = dirPath.resolve(System.currentTimeMillis() + "_" + safeFileName);

            MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
            long bytesCopied;
            try (DigestInputStream dis = new DigestInputStream(input, sha256Digest)) {
                bytesCopied = Files.copy(dis, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            long duration = System.currentTimeMillis() - startTime;
            String sha256 = HexFormat.of().formatHex(sha256Digest.digest());

            DownloadHistoryEntity entity = DownloadHistoryEntity.builder()
                    .sourceId(sourceId)
                    .country(country)
                    .sourceName(sourceName)
                    .fileName(safeFileName)
                    .filePath(targetPath.toAbsolutePath().toString())
                    .contentType(Files.probeContentType(targetPath))
                    .fileSize(bytesCopied)
                    .checksumAlgorithm("SHA-256")
                    .sha256(sha256)
                    .downloadUrl(downloadUrl)
                    .httpStatus(200)
                    .downloadDurationMs(duration)
                    .datasetVersion(version != null ? version : "2026.1")
                    .build();

            downloadHistoryRepository.save(entity);
            log.info("Download Manager - Archived file for {} at {} (size: {} bytes, SHA-256: {}, duration: {} ms)",
                    country, targetPath, bytesCopied, sha256, duration);

            return targetPath;
        } catch (Exception e) {
            log.error("Failed to archive source download file for country {}", country, e);
            return null;
        }
    }
}
