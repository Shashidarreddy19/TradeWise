package com.cbec.ai.pipeline.ukregulation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cbec.ai.pipeline.model.entity.RegulationDownloadHistoryEntity;
import com.cbec.ai.pipeline.model.entity.RegulationRawEntity;
import com.cbec.ai.pipeline.model.entity.RegulationSourceEntity;
import com.cbec.ai.pipeline.repository.RegulationRawRepository;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class UkRegulationExtractorService {

    private final RegulationRawRepository rawRepository;

    public UkRegulationExtractorService(RegulationRawRepository rawRepository) {
        this.rawRepository = rawRepository;
    }

    @Data
    @Builder
    public static class ExtractionResult {
        private String format;
        private int sectionsExtracted;
        private int pagesExtracted;
        private long recordsInserted;
        private boolean success;
    }

    /**
     * Extracts text from downloaded HTML, PDF, or API regulation payload and persists into `regulation_raw`.
     */
    public ExtractionResult extractAndSaveRegulationText(
            UkRegulationDownloaderService.DownloadResult downloadResult) {

        if (!downloadResult.isSuccess() || downloadResult.getLocalFilePath() == null) {
            return ExtractionResult.builder().format("NONE").recordsInserted(0).success(false).build();
        }

        RegulationSourceEntity source = downloadResult.getSource();
        RegulationDownloadHistoryEntity downloadHistory = downloadResult.getDownloadHistory();
        Path filePath = downloadResult.getLocalFilePath();
        File file = filePath.toFile();

        String format = source.getFormat() != null ? source.getFormat().toUpperCase() : "HTML";

        log.info("Extracting regulation text for source ID {}, Title: '{}', Format: {}", source.getId(), source.getTitle(), format);

        if ("PDF".equals(format) || file.getName().endsWith(".pdf")) {
            return extractPdfDocument(source, downloadHistory, file);
        } else if ("JSON".equals(format) || "API".equals(format) || file.getName().endsWith(".json")) {
            return extractJsonDocument(source, downloadHistory, file, downloadResult.getContent());
        } else {
            return extractHtmlDocument(source, downloadHistory, file, downloadResult.getContent());
        }
    }

    private ExtractionResult extractJsonDocument(
            RegulationSourceEntity source,
            RegulationDownloadHistoryEntity downloadHistory,
            File file,
            byte[] rawBytes) {

        long recordsInserted = 0;
        int itemsFound = 0;

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = rawBytes != null ? mapper.readTree(rawBytes) : mapper.readTree(file);

            List<RegulationRawEntity> batch = new ArrayList<>();

            JsonNode dataNode = rootNode.path("data");
            if (dataNode.isArray()) {
                for (JsonNode item : dataNode) {
                    itemsFound++;
                    String id = item.path("id").asText("");
                    String type = item.path("type").asText("JSON_ITEM");
                    JsonNode attributes = item.path("attributes");

                    String title = attributes.path("title").asText(attributes.path("name").asText(type));
                    String numeral = attributes.path("numeral").asText("");
                    String chapterFrom = attributes.path("chapter_from").asText("");
                    String chapterTo = attributes.path("chapter_to").asText("");

                    String rawText = String.format("Section %s (%s): %s [Chapters %s - %s]",
                            numeral, id, title, chapterFrom, chapterTo);

                    RegulationRawEntity raw = RegulationRawEntity.builder()
                            .country("United Kingdom")
                            .authority(source.getAuthority())
                            .title(source.getTitle())
                            .documentType(source.getDocumentType())
                            .section("Section " + numeral)
                            .subsection(type)
                            .pageNumber(1)
                            .rawText(rawText)
                            .sourceUrl(source.getSourceUrl())
                            .downloadId(downloadHistory != null ? downloadHistory.getId() : null)
                            .build();

                    batch.add(raw);
                }
            }

            if (!batch.isEmpty()) {
                rawRepository.saveAll(batch);
                recordsInserted += batch.size();
            }

            log.info("Extracted JSON regulation text for source ID {}. Items: {}, Records Inserted: {}",
                    source.getId(), itemsFound, recordsInserted);

            return ExtractionResult.builder()
                    .format("JSON")
                    .sectionsExtracted(itemsFound)
                    .pagesExtracted(1)
                    .recordsInserted(recordsInserted)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Error extracting JSON regulation document for source ID {}", source.getId(), e);
            return ExtractionResult.builder().format("JSON").recordsInserted(0).success(false).build();
        }
    }

    private ExtractionResult extractHtmlDocument(
            RegulationSourceEntity source,
            RegulationDownloadHistoryEntity downloadHistory,
            File file,
            byte[] rawBytes) {

        long recordsInserted = 0;
        int sectionsFound = 0;

        try {
            String html = rawBytes != null ? new String(rawBytes, StandardCharsets.UTF_8) : Jsoup.parse(file, "UTF-8").html();
            Document doc = Jsoup.parse(html);

            // Remove clutter: headers, footers, navigation, cookies, scripts, ads, styles
            doc.select("header, footer, nav, script, style, iframe, noscript, .cookie-banner, .navigation, #footer, #header").remove();

            String currentSection = "General Regulations";
            String currentSubsection = "";

            Elements elements = doc.select("h1, h2, h3, h4, h5, p, li, table tr");
            List<RegulationRawEntity> batch = new ArrayList<>();

            for (Element el : elements) {
                String tagName = el.tagName().toLowerCase();
                String text = el.text().trim();

                if (text.isEmpty()) continue;

                if (tagName.startsWith("h")) {
                    currentSection = text;
                    currentSubsection = tagName.toUpperCase();
                    sectionsFound++;
                } else {
                    RegulationRawEntity raw = RegulationRawEntity.builder()
                            .country("United Kingdom")
                            .authority(source.getAuthority())
                            .title(source.getTitle())
                            .documentType(source.getDocumentType())
                            .section(currentSection)
                            .subsection(currentSubsection)
                            .pageNumber(1)
                            .rawText(text)
                            .sourceUrl(source.getSourceUrl())
                            .downloadId(downloadHistory != null ? downloadHistory.getId() : null)
                            .build();

                    batch.add(raw);
                }

                if (batch.size() >= 500) {
                    rawRepository.saveAll(batch);
                    recordsInserted += batch.size();
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                rawRepository.saveAll(batch);
                recordsInserted += batch.size();
                batch.clear();
            }

            log.info("Extracted HTML regulation text for source ID {}. Sections: {}, Records Inserted: {}",
                    source.getId(), sectionsFound, recordsInserted);

            return ExtractionResult.builder()
                    .format("HTML")
                    .sectionsExtracted(sectionsFound)
                    .pagesExtracted(1)
                    .recordsInserted(recordsInserted)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Error extracting HTML regulation document for source ID {}", source.getId(), e);
            return ExtractionResult.builder().format("HTML").recordsInserted(0).success(false).build();
        }
    }

    private ExtractionResult extractPdfDocument(
            RegulationSourceEntity source,
            RegulationDownloadHistoryEntity downloadHistory,
            File file) {

        long recordsInserted = 0;
        int totalPages = 0;

        try (PDDocument doc = Loader.loadPDF(file)) {
            totalPages = doc.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();

            List<RegulationRawEntity> batch = new ArrayList<>();

            for (int page = 1; page <= totalPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(doc);

                if (pageText == null || pageText.trim().isEmpty()) continue;

                String[] paragraphs = pageText.split("\r?\n\r?\n");
                for (String pText : paragraphs) {
                    String clean = pText.trim();
                    if (clean.isEmpty()) continue;

                    RegulationRawEntity raw = RegulationRawEntity.builder()
                            .country("United Kingdom")
                            .authority(source.getAuthority())
                            .title(source.getTitle())
                            .documentType(source.getDocumentType())
                            .section("Page " + page)
                            .subsection("PDF Section")
                            .pageNumber(page)
                            .rawText(clean)
                            .sourceUrl(source.getSourceUrl())
                            .downloadId(downloadHistory != null ? downloadHistory.getId() : null)
                            .build();

                    batch.add(raw);
                }

                if (batch.size() >= 500) {
                    rawRepository.saveAll(batch);
                    recordsInserted += batch.size();
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                rawRepository.saveAll(batch);
                recordsInserted += batch.size();
                batch.clear();
            }

            log.info("Extracted PDF regulation text for source ID {}. Pages: {}, Records Inserted: {}",
                    source.getId(), totalPages, recordsInserted);

            return ExtractionResult.builder()
                    .format("PDF")
                    .sectionsExtracted(totalPages)
                    .pagesExtracted(totalPages)
                    .recordsInserted(recordsInserted)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Error extracting PDF regulation document for source ID {}", source.getId(), e);
            return ExtractionResult.builder().format("PDF").recordsInserted(0).success(false).build();
        }
    }
}
