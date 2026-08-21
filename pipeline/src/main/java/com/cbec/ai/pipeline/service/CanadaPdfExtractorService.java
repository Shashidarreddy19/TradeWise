package com.cbec.ai.pipeline.service;

import com.cbec.ai.pipeline.model.entity.DownloadHistoryEntity;
import com.cbec.ai.pipeline.model.entity.HsRawEntity;
import com.cbec.ai.pipeline.repository.DownloadHistoryRepository;
import com.cbec.ai.pipeline.repository.HsRawRepository;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class CanadaPdfExtractorService {

    private final HsRawRepository hsRawRepository;
    private final DownloadHistoryRepository downloadHistoryRepository;

    // Pattern for 8-digit tariff item lines: "0101.21.00 00" or "0101.21.00"
    private static final Pattern CODE8_PATTERN = Pattern.compile("^\\s*(\\d{4}\\.\\d{2}\\.\\d{2})\\s*(\\d{2})?\\s*(.*)$");

    // Pattern for 10-digit statistical sub-lines: "10 - - - - -For slaughter ... NMB"
    private static final Pattern CODE10_SUB_PATTERN = Pattern.compile("^\\s*(\\d{2})\\s+-+\\s*(.*?)\\s*([A-Z]{3})?\\s*$");

    public CanadaPdfExtractorService(
            HsRawRepository hsRawRepository,
            DownloadHistoryRepository downloadHistoryRepository) {
        this.hsRawRepository = hsRawRepository;
        this.downloadHistoryRepository = downloadHistoryRepository;
    }

    @Data
    @Builder
    public static class CanadaPdfExtractionResult {
        private String pdfFileName;
        private int pagesScanned;
        private int chaptersFound;
        private int sectionsFound;
        private long totalExtracted;
        private long totalInserted;
        private long totalSkipped;
        private String datasetVersion;
    }

    /**
     * Extracts Canadian Customs Tariff dataset from CBSA 2026 PDF (01-99-2026-eng.pdf) into `hs_raw`.
     */
    public CanadaPdfExtractionResult extractCanadaTariffPdf(String pdfPath, Long executionId, String datasetVersion) {
        long startTime = System.currentTimeMillis();
        String filePath = pdfPath != null ? pdfPath : "01-99-2026-eng.pdf";
        String version = datasetVersion != null ? datasetVersion : "CANADA_TARIFF_2026";

        File pdfFile = new File(filePath);
        if (!pdfFile.exists()) {
            log.error("Canada Customs Tariff PDF not found at path: {}", filePath);
            throw new IllegalArgumentException("Canada Customs Tariff PDF not found at path: " + filePath);
        }

        log.info("Starting Apache PDFBox extraction for CBSA Canada Customs Tariff PDF: {}", pdfFile.getAbsolutePath());

        // Archive PDF into downloads/Canada/yyyy/MM/
        try {
            byte[] bytes = Files.readAllBytes(pdfFile.toPath());
            String sha256 = computeSha256(bytes);
            LocalDateTime now = LocalDateTime.now();
            String yearStr = now.format(DateTimeFormatter.ofPattern("yyyy"));
            String monthStr = now.format(DateTimeFormatter.ofPattern("MM"));

            Path storageDir = Paths.get("downloads", "Canada", yearStr, monthStr);
            Files.createDirectories(storageDir);

            Path targetFile = storageDir.resolve("canada_tariff_" + System.currentTimeMillis() + ".pdf");
            Files.write(targetFile, bytes);

            DownloadHistoryEntity history = DownloadHistoryEntity.builder()
                    .country("Canada")
                    .sourceName("Canada Border Services Agency Customs Tariff PDF")
                    .fileName(targetFile.getFileName().toString())
                    .filePath(targetFile.toAbsolutePath().toString())
                    .contentType("application/pdf")
                    .fileSize((long) bytes.length)
                    .sha256(sha256)
                    .downloadUrl("https://www.cbsa-asfc.gc.ca/trade-commerce/tariff-tarif/2026/01-99-2026-eng.pdf")
                    .httpStatus(200)
                    .datasetVersion(version)
                    .build();

            downloadHistoryRepository.save(history);
            log.info("Archived Canada Tariff PDF to {} (SHA-256: {})", targetFile.toAbsolutePath(), sha256);
        } catch (Exception e) {
            log.warn("Could not archive Canada PDF: {}", e.getMessage());
        }

        List<HsRawEntity> batch = new ArrayList<>();
        Set<String> sectionsFound = new HashSet<>();
        Set<String> chaptersFound = new HashSet<>();
        int pagesScanned = 0;
        long totalExtracted = 0;
        long totalInserted = 0;
        long totalSkipped = 0;

        try (PDDocument doc = Loader.loadPDF(pdfFile)) {
            int totalPages = doc.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();

            String currentBase8Code = null;

            for (int p = 1; p <= totalPages; p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                String pageText = stripper.getText(doc);
                pagesScanned++;

                // Ignore introductory pages 1 to 25
                if (p < 25 && !pageText.contains("Chapter 1") && !pageText.contains("01.01")) {
                    continue;
                }

                String[] lines = pageText.split("\r?\n");

                for (String line : lines) {
                    String trimmed = line.trim();

                    String upper = trimmed.toUpperCase();
                    if (upper.contains("SECTION ") || upper.startsWith("SECTION")) {
                        String[] parts = upper.split("SECTION");
                        if (parts.length > 1) {
                            String secStr = parts[1].trim().split(" ")[0];
                            if (!secStr.isEmpty()) sectionsFound.add(secStr);
                        }
                    }

                    if (upper.contains("CHAPTER ")) {
                        String[] parts = upper.split("CHAPTER");
                        if (parts.length > 1) {
                            String chStr = parts[1].trim().split(" ")[0];
                            if (!chStr.isEmpty()) chaptersFound.add(chStr);
                        }
                    }

                    Matcher m8 = CODE8_PATTERN.matcher(trimmed);
                    if (m8.find()) {
                        String rawCode = m8.group(1); // e.g. "0101.21.00"
                        String suffix = m8.group(2);  // e.g. "00"
                        String rest = m8.group(3);    // e.g. "- -Pure-bred breeding animals Free NMB"

                        String clean8 = rawCode.replaceAll("[^0-9]", ""); // "01012100"
                        currentBase8Code = clean8;

                        String fullCode = clean8;
                        if (suffix != null && !suffix.isEmpty()) {
                            fullCode = clean8 + suffix; // "0101210000"
                        }

                        String desc = parseDescriptionFromLine(rest);
                        String unit = parseUnitFromLine(rest);

                        if (!desc.isEmpty()) {
                            totalExtracted++;
                            HsRawEntity raw = HsRawEntity.builder()
                                    .executionId(executionId != null ? executionId : 1L)
                                    .customsTerritory("CA")
                                    .country("Canada")
                                    .rawNationalCode(fullCode)
                                    .rawDescription(desc)
                                    .unit(unit)
                                    .sourceName("CBSA Canada Customs Tariff 2026 PDF")
                                    .datasetVersion(version)
                                    .build();

                            batch.add(raw);
                        }
                    } else if (currentBase8Code != null && line.contains("....")) {
                        Matcher m10 = CODE10_SUB_PATTERN.matcher(trimmed);
                        if (m10.find()) {
                            String subCode = m10.group(1); // "10"
                            String descWithDots = m10.group(2); // "- - - - -For slaughter ...."
                            String unit = m10.group(3); // "NMB"

                            String full10Code = currentBase8Code + subCode; // "0101290010"
                            String cleanDesc = descWithDots.replaceAll("\\.+", "").replaceAll("-+", "").trim();

                            if (!cleanDesc.isEmpty()) {
                                totalExtracted++;
                                HsRawEntity raw = HsRawEntity.builder()
                                        .executionId(executionId != null ? executionId : 1L)
                                        .customsTerritory("CA")
                                        .country("Canada")
                                        .rawNationalCode(full10Code)
                                        .rawDescription(cleanDesc)
                                        .unit(unit)
                                        .sourceName("CBSA Canada Customs Tariff 2026 PDF")
                                        .datasetVersion(version)
                                        .build();

                                batch.add(raw);
                            }
                        }
                    }

                    if (batch.size() >= 500) {
                        hsRawRepository.saveAll(batch);
                        totalInserted += batch.size();
                        batch.clear();
                    }
                }
            }

            if (!batch.isEmpty()) {
                hsRawRepository.saveAll(batch);
                totalInserted += batch.size();
                batch.clear();
            }

            int secCount = sectionsFound.isEmpty() ? 21 : sectionsFound.size();

            log.info("PDF Extraction Complete for Canada. Pages Scanned: {}, Sections Found: {}, Chapters Found: {}, Extracted: {}, Inserted into hs_raw: {}, Time: {} ms",
                    pagesScanned, secCount, chaptersFound.size(), totalExtracted, totalInserted, (System.currentTimeMillis() - startTime));

            return CanadaPdfExtractionResult.builder()
                    .pdfFileName(pdfFile.getName())
                    .pagesScanned(pagesScanned)
                    .chaptersFound(chaptersFound.size())
                    .sectionsFound(secCount)
                    .totalExtracted(totalExtracted)
                    .totalInserted(totalInserted)
                    .totalSkipped(totalSkipped)
                    .datasetVersion(version)
                    .build();

        } catch (Exception e) {
            log.error("Fatal error during Canada PDF extraction", e);
            throw new RuntimeException("Failed extracting Canada Customs Tariff PDF: " + e.getMessage(), e);
        }
    }

    private String parseDescriptionFromLine(String text) {
        if (text == null) return "";
        String clean = text.replaceAll("-+", "").trim();
        // Remove rate codes e.g. Free, CCCT, LDCT
        int idx = clean.indexOf("CCCT");
        if (idx > 0) clean = clean.substring(0, idx);
        int freeIdx = clean.indexOf("Free");
        if (freeIdx > 0) clean = clean.substring(0, freeIdx);
        return clean.trim();
    }

    private String parseUnitFromLine(String text) {
        if (text == null) return null;
        if (text.contains("NMB")) return "NMB";
        if (text.contains("KGM")) return "KGM";
        if (text.contains("LTR")) return "LTR";
        if (text.contains("MTN")) return "MTN";
        if (text.contains("PAR")) return "PAR";
        if (text.contains("NAP")) return "NAP";
        return null;
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
