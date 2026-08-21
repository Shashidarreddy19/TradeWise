package com.cbec.ai.pipeline.service;

import com.cbec.ai.pipeline.model.entity.HsRawEntity;
import com.cbec.ai.pipeline.model.entity.PipelineExecutionEntity;
import com.cbec.ai.pipeline.repository.HsRawRepository;
import com.cbec.ai.pipeline.repository.PipelineExecutionRepository;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Production Indian ITC-HS PDF Extractor.
 * Extracts ALL valid Indian tariff codes (8-10 digits) from the official ITC-HS PDF.
 * 
 * Key improvements over previous version:
 * 1. Supports 8-digit AND 10-digit national codes
 * 2. Tracks current chapter across pages (multi-page table support)
 * 3. Joins multi-line descriptions
 * 4. Multiple HS code format patterns
 * 5. Robust chapter detection
 * 6. Diagnostic logging per chapter
 */
@Slf4j
@Service
public class IndiaItcHsPdfExtractorService {

    private final HsRawRepository hsRawRepository;
    private final PipelineExecutionRepository pipelineExecutionRepository;

    // HS code patterns — support 8-digit and 10-digit codes with various spacing
    // Pattern: "0904 11 00 20" or "0904 1100 20" or "09041100 20"
    private static final Pattern HS10_SPACED = Pattern.compile(
            "^(\\d{4})\\s+(\\d{2})\\s+(\\d{2})\\s+(\\d{2})\\b(.*)$");
    // Pattern: "0904 11 00" (8-digit spaced)
    private static final Pattern HS8_SPACED = Pattern.compile(
            "^(\\d{4})\\s+(\\d{2})\\s+(\\d{2})\\b(.*)$");
    // Pattern: "09041100" or "0904110020" (compact 8 or 10)
    private static final Pattern HS_COMPACT = Pattern.compile(
            "^(\\d{8,10})\\b(.*)$");
    // Pattern: "0904.11.00.20" (dotted)
    private static final Pattern HS_DOTTED = Pattern.compile(
            "^(\\d{4})[.\\-](\\d{2})[.\\-](\\d{2})(?:[.\\-](\\d{2}))?\\b(.*)$");

    // Chapter detection patterns
    private static final Pattern CHAPTER_PATTERN = Pattern.compile(
            "(?i)^\\s*(?:CHAPTER|Chapter)\\s+(\\d{1,2})\\b");
    private static final Pattern CHAPTER_HEADING_PATTERN = Pattern.compile(
            "^\\s*(\\d{1,2})\\s+[A-Z][A-Za-z].*(?:animals|meat|fish|dairy|vegetable|fruit|coffee|cereal|" +
            "milling|oil|gum|lac|vegetable|fats|food|sugar|cocoa|preparations|beverages|residues|" +
            "tobacco|salt|ores|mineral|chemical|pharmaceutical|fertilizer|tanning|essential|soap|" +
            "albuminoidal|explosive|photographic|miscellaneous|plastic|rubber|raw|leather|fur|wood|cork|" +
            "straw|pulp|paper|printed|silk|wool|cotton|textile|wadding|knitted|apparel|" +
            "footwear|headgear|umbrella|feather|stone|ceramic|glass|pearl|iron|steel|copper|nickel|" +
            "aluminium|lead|zinc|tin|metal|tool|machinery|electrical|railway|vehicle|aircraft|ship|" +
            "optical|clock|musical|arms|furniture|toy|art|special).*$",
            Pattern.CASE_INSENSITIVE);

    // Lines to skip
    private static final Set<String> SKIP_PREFIXES = Set.of(
            "SECTION", "NOTES:", "NOTE:", "FOR OFFICIAL USE", "INDIAN TRADE CLASSIFICATION",
            "HARMONISED COMMODITY", "THE FIRST SCHEDULE", "GOVERNMENT OF INDIA",
            "MINISTRY OF COMMERCE", "DIRECTORATE GENERAL", "SUPPLEMENTARY NOTES",
            "GENERAL EXPLANATORY", "SUB-HEADING NOTES");

    private static final Set<String> KNOWN_UNITS = Set.of(
            "kg", "kg.", "u", "u.", "nos", "nos.", "carat", "c/k",
            "ltr", "ltr.", "l", "m", "m.", "sqm", "sqm.", "cbm",
            "t", "mt", "mt.", "pa", "tu", "doz", "doz.", "g", "g.",
            "1000", "thm", "kw", "kwh", "pcs", "pcs.", "m2", "m3");

    public IndiaItcHsPdfExtractorService(
            HsRawRepository hsRawRepository,
            PipelineExecutionRepository pipelineExecutionRepository) {
        this.hsRawRepository = hsRawRepository;
        this.pipelineExecutionRepository = pipelineExecutionRepository;
    }

    @Data
    @Builder
    public static class PdfExtractionSummary {
        private String pdfPath;
        private int totalPagesProcessed;
        private long totalRowsExtracted;
        private long totalRowsSkipped;
        private long totalRowsInserted;
        private long executionTimeMs;
        private String status;
        private Long executionId;
        private Map<String, Integer> chapterCounts;
        private List<String> detectedChapters;
        private List<String> missingChapters;
    }

    @Data
    @Builder
    public static class ExtractedTariffLine {
        private String nationalCode;
        private String description;
        private String unit;
        private String chapter;
        private int pageNumber;
    }

    /**
     * Extract ALL valid Indian ITC(HS) tariff codes from the official PDF.
     * Supports 8-digit and 10-digit codes with multi-page table handling.
     */
    public PdfExtractionSummary extractItcHsPdf(String pdfFilePath) {
        long startTime = System.currentTimeMillis();
        log.info("Starting IMPROVED PDF extraction for Indian ITC(HS) from: {}", pdfFilePath);

        File pdfFile = new File(pdfFilePath);
        if (!pdfFile.exists()) {
            throw new IllegalArgumentException("PDF file not found: " + pdfFilePath);
        }

        PipelineExecutionEntity execution = PipelineExecutionEntity.builder()
                .pipelineName("India ITC(HS) PDF Extraction (Improved)")
                .country("India")
                .source("ITC-HS_2022.pdf")
                .startedAt(LocalDateTime.now())
                .status("RUNNING")
                .recordsFound(0L)
                .recordsInserted(0L)
                .recordsUpdated(0L)
                .duplicates(0L)
                .invalidRecords(0L)
                .build();
        execution = pipelineExecutionRepository.save(execution);

        int totalPages = 0;
        long rowsExtracted = 0;
        long rowsSkipped = 0;
        long rowsInserted = 0;
        Set<String> seenCodes = new HashSet<>();
        Map<String, Integer> chapterCounts = new TreeMap<>();
        String currentChapter = null;
        String pendingDescription = null;
        String pendingCode = null;
        String pendingUnit = null;

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            totalPages = document.getNumberOfPages();
            log.info("Opened ITC(HS) PDF: {} pages", totalPages);

            PDFTextStripper stripper = new PDFTextStripper();
            List<HsRawEntity> batch = new ArrayList<>();

            for (int page = 1; page <= totalPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document);
                String[] lines = pageText.split("\\r?\\n");

                for (String line : lines) {
                    if (line == null) continue;
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) continue;

                    // Check for chapter heading
                    String detectedChapter = detectChapter(trimmed);
                    if (detectedChapter != null) {
                        // Save pending record before switching chapters
                        if (pendingCode != null && pendingDescription != null) {
                            HsRawEntity entity = buildEntity(pendingCode, pendingDescription,
                                    pendingUnit, currentChapter, execution.getId());
                            if (entity != null && !seenCodes.contains(pendingCode)) {
                                seenCodes.add(pendingCode);
                                batch.add(entity);
                                rowsExtracted++;
                                chapterCounts.merge(currentChapter != null ? currentChapter : "??", 1, Integer::sum);
                            }
                            pendingCode = null;
                            pendingDescription = null;
                            pendingUnit = null;
                        }
                        currentChapter = detectedChapter;
                        continue;
                    }

                    // Skip known non-data lines
                    if (shouldSkipLine(trimmed)) {
                        rowsSkipped++;
                        continue;
                    }

                    // Try to extract HS code from this line
                    ExtractedTariffLine tariffLine = parseTariffLine(trimmed);

                    if (tariffLine != null) {
                        // Save the previous pending record
                        if (pendingCode != null && pendingDescription != null) {
                            HsRawEntity entity = buildEntity(pendingCode, pendingDescription,
                                    pendingUnit, currentChapter, execution.getId());
                            if (entity != null && !seenCodes.contains(pendingCode)) {
                                seenCodes.add(pendingCode);
                                batch.add(entity);
                                rowsExtracted++;
                                chapterCounts.merge(currentChapter != null ? currentChapter : "??", 1, Integer::sum);
                            }
                        }
                        // Start new pending record
                        pendingCode = tariffLine.getNationalCode();
                        pendingDescription = tariffLine.getDescription();
                        pendingUnit = tariffLine.getUnit();
                    } else if (pendingCode != null && !trimmed.isEmpty()) {
                        // This line might be a continuation of the previous description
                        if (isDescriptionContinuation(trimmed)) {
                            pendingDescription = (pendingDescription != null ? pendingDescription + " " : "") + trimmed;
                        }
                    } else {
                        rowsSkipped++;
                    }
                }

                // Batch save every 500 records
                if (batch.size() >= 500) {
                    hsRawRepository.saveAll(batch);
                    rowsInserted += batch.size();
                    batch.clear();
                }

                if (page % 100 == 0) {
                    log.info("Processed {}/{} pages | extracted: {} | chapters: {}",
                            page, totalPages, rowsExtracted, chapterCounts.size());
                }
            }

            // Save last pending record
            if (pendingCode != null && pendingDescription != null) {
                HsRawEntity entity = buildEntity(pendingCode, pendingDescription,
                        pendingUnit, currentChapter, execution.getId());
                if (entity != null && !seenCodes.contains(pendingCode)) {
                    seenCodes.add(pendingCode);
                    batch.add(entity);
                    rowsExtracted++;
                    chapterCounts.merge(currentChapter != null ? currentChapter : "??", 1, Integer::sum);
                }
            }

            // Save remaining batch
            if (!batch.isEmpty()) {
                hsRawRepository.saveAll(batch);
                rowsInserted += batch.size();
            }

            execution.setStatus("COMPLETED");
            log.info("PDF extraction complete. Chapters detected: {}", chapterCounts.keySet());

        } catch (Exception e) {
            log.error("PDF extraction failed", e);
            execution.setStatus("FAILED");
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            execution.setCompletedAt(LocalDateTime.now());
            execution.setExecutionTimeMs(duration);
            execution.setRecordsFound(rowsExtracted);
            execution.setRecordsInserted(rowsInserted);
            execution.setInvalidRecords(rowsSkipped);
            pipelineExecutionRepository.save(execution);

            log.info("========================================");
            log.info("INDIA ITC-HS PDF EXTRACTION REPORT");
            log.info("Pages: {} | Extracted: {} | Inserted: {} | Skipped: {}",
                    totalPages, rowsExtracted, rowsInserted, rowsSkipped);
            log.info("Chapters detected: {}", chapterCounts.size());
            chapterCounts.forEach((ch, cnt) -> log.info("  Chapter {}: {} codes", ch, cnt));
            log.info("========================================");
        }

        // Determine missing chapters
        List<String> detected = new ArrayList<>(chapterCounts.keySet());
        List<String> missing = new ArrayList<>();
        for (int i = 1; i <= 99; i++) {
            String ch = String.format("%02d", i);
            if (!chapterCounts.containsKey(ch)) missing.add(ch);
        }

        return PdfExtractionSummary.builder()
                .pdfPath(pdfFilePath)
                .totalPagesProcessed(totalPages)
                .totalRowsExtracted(rowsExtracted)
                .totalRowsSkipped(rowsSkipped)
                .totalRowsInserted(rowsInserted)
                .executionTimeMs(System.currentTimeMillis() - startTime)
                .status(execution.getStatus())
                .executionId(execution.getId())
                .chapterCounts(chapterCounts)
                .detectedChapters(detected)
                .missingChapters(missing)
                .build();
    }

    /**
     * Detect chapter number from a line of text.
     * Supports: "Chapter 01", "CHAPTER 1", "01 LIVE ANIMALS" etc.
     */
    private String detectChapter(String line) {
        // Pattern 1: "Chapter X" or "CHAPTER X"
        Matcher m1 = CHAPTER_PATTERN.matcher(line);
        if (m1.find()) {
            int num = Integer.parseInt(m1.group(1));
            if (num >= 1 && num <= 99) return String.format("%02d", num);
        }
        // Pattern 2: "01 LIVE ANIMALS" — number followed by uppercase section title
        Matcher m2 = CHAPTER_HEADING_PATTERN.matcher(line);
        if (m2.find()) {
            int num = Integer.parseInt(m2.group(1));
            if (num >= 1 && num <= 99) return String.format("%02d", num);
        }
        return null;
    }

    /**
     * Parse a tariff line to extract HS code and description.
     * Supports 8-digit and 10-digit codes with various spacing.
     */
    private ExtractedTariffLine parseTariffLine(String line) {
        String code = null;
        String remainder = null;

        // Try 10-digit spaced: "0904 11 00 20"
        Matcher m10 = HS10_SPACED.matcher(line);
        if (m10.find()) {
            code = m10.group(1) + m10.group(2) + m10.group(3) + m10.group(4);
            remainder = m10.group(5);
        }

        // Try 8-digit spaced: "0904 11 00"
        if (code == null) {
            Matcher m8 = HS8_SPACED.matcher(line);
            if (m8.find()) {
                code = m8.group(1) + m8.group(2) + m8.group(3);
                remainder = m8.group(4);
            }
        }

        // Try dotted: "0904.11.00.20"
        if (code == null) {
            Matcher md = HS_DOTTED.matcher(line);
            if (md.find()) {
                code = md.group(1) + md.group(2) + md.group(3);
                if (md.group(4) != null) code += md.group(4);
                remainder = md.group(5);
            }
        }

        // Try compact: "09041100" or "0904110020"
        if (code == null) {
            Matcher mc = HS_COMPACT.matcher(line);
            if (mc.find()) {
                code = mc.group(1);
                remainder = mc.group(2);
            }
        }

        if (code == null) return null;

        // Validate code length (8 or 10 digits)
        if (code.length() != 8 && code.length() != 10) return null;

        // Validate it's not a date, phone number, or page number
        if (code.startsWith("0000") || code.startsWith("9999")) return null;
        String chapter = code.substring(0, 2);
        int chapterNum = Integer.parseInt(chapter);
        if (chapterNum < 1 || chapterNum > 99) return null;

        // Clean remainder to get description
        String cleanedRemainder = (remainder != null) ? remainder.trim() : "";
        cleanedRemainder = cleanedRemainder.replaceAll("^[\\-\\:\\s]+", "").trim();

        // Extract unit and clean description
        String unit = extractUnit(cleanedRemainder);
        String description = cleanDescription(cleanedRemainder, unit);

        // Pad 8-digit to 10-digit with trailing zeros for consistency
        if (code.length() == 8) code = code + "00";

        return ExtractedTariffLine.builder()
                .nationalCode(code)
                .description(description.isEmpty() ? null : description)
                .unit(unit)
                .chapter(chapter)
                .build();
    }

    /**
     * Check if a line is a continuation of a description (not a new HS code or header).
     */
    private boolean isDescriptionContinuation(String line) {
        // Not a new HS code
        if (HS_COMPACT.matcher(line).find()) return false;
        if (HS8_SPACED.matcher(line).find()) return false;
        if (HS10_SPACED.matcher(line).find()) return false;
        // Not a chapter heading
        if (CHAPTER_PATTERN.matcher(line).find()) return false;
        // Not a skip line
        if (shouldSkipLine(line)) return false;
        // Not just a number (page number)
        if (line.matches("^\\d{1,4}$")) return false;
        // Must contain alphabetic content
        return line.matches(".*[a-zA-Z].*");
    }

    private boolean shouldSkipLine(String line) {
        String upper = line.toUpperCase().trim();
        for (String prefix : SKIP_PREFIXES) {
            if (upper.startsWith(prefix)) return true;
        }
        // Table column headers: "(1) (2) (3) (4)"
        if (upper.matches("^\\(\\d\\)\\s+\\(\\d\\).*")) return true;
        // Pure page numbers
        if (upper.matches("^\\d{1,4}$")) return true;
        // Duty rate lines: "30% -" or "Free"
        if (upper.matches("^\\d+%.*") || upper.equals("FREE")) return true;
        return false;
    }

    private HsRawEntity buildEntity(String code, String description, String unit,
                                     String chapter, Long executionId) {
        if (code == null || description == null || description.isBlank()) return null;
        if (code.length() != 10) return null; // After padding, should be 10 digits

        return HsRawEntity.builder()
                .executionId(executionId)
                .customsTerritory("IN")
                .country("India")
                .rawNationalCode(code)
                .rawDescription(description.length() > 500 ? description.substring(0, 500) : description)
                .unit(unit)
                .sourceName("ITC-HS 2022 Official PDF")
                .datasetVersion("ITC_HS_2022")
                .build();
    }

    private String extractUnit(String text) {
        if (text == null || text.isEmpty()) return null;
        String[] tokens = text.split("\\s+");
        for (String token : tokens) {
            String lower = token.toLowerCase().replaceAll("[^a-z0-9./]", "");
            if (KNOWN_UNITS.contains(lower)) {
                return token.replaceAll("[^a-zA-Z0-9.]", "");
            }
        }
        return null;
    }

    private String cleanDescription(String text, String extractedUnit) {
        if (text == null) return "";
        // Remove duty percentages and "Free"
        String cleaned = text.replaceAll("\\b\\d+%", "")
                .replaceAll("\\bFree\\b", "")
                .replaceAll("\\s*-\\s*$", "")
                .trim();
        // Remove unit from description
        if (extractedUnit != null) {
            cleaned = cleaned.replaceAll("\\b" + Pattern.quote(extractedUnit) + "\\b", "").trim();
        }
        // Clean extra spaces
        return cleaned.replaceAll("\\s+", " ").trim();
    }
}
