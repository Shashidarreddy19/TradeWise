package com.cbec.ai.pipeline.regulation.service;

import com.cbec.ai.pipeline.model.entity.RegulationDownloadHistoryEntity;
import com.cbec.ai.pipeline.model.entity.RegulationRawEntity;
import com.cbec.ai.pipeline.model.entity.RegulationSourceEntity;
import com.cbec.ai.pipeline.repository.RegulationRawRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class RegulationExtractorService {

    private final RegulationRawRepository rawRepository;
    private final ObjectMapper objectMapper;

    public RegulationExtractorService(RegulationRawRepository rawRepository, ObjectMapper objectMapper) {
        this.rawRepository = rawRepository;
        this.objectMapper = objectMapper;
    }

    @Data
    @Builder
    public static class ExtractionResult {
        private String country;
        private String format;
        private int sectionsExtracted;
        private int pagesExtracted;
        private long recordsInserted;
        private boolean success;
    }

    public ExtractionResult extractAndSaveRegulationText(RegulationDownloaderService.DownloadResult downloadResult) {
        if (!downloadResult.isSuccess() || downloadResult.getLocalFilePath() == null) {
            return ExtractionResult.builder().country(downloadResult.getCountry()).format("NONE").recordsInserted(0).success(false).build();
        }

        RegulationSourceEntity source = downloadResult.getSource();
        RegulationDownloadHistoryEntity downloadHistory = downloadResult.getDownloadHistory();
        File file = downloadResult.getLocalFilePath().toFile();

        String format = source.getFormat() != null ? source.getFormat().toUpperCase() : "HTML";

        log.info("Extracting regulation text for country: '{}', Source ID: {}, Title: '{}', Format: {}",
                downloadResult.getCountry(), source.getId(), source.getTitle(), format);

        if ("PDF".equals(format) || file.getName().endsWith(".pdf")) {
            return extractPdfDocument(downloadResult.getCountry(), source, downloadHistory, file);
        } else if ("JSON".equals(format) || "API".equals(format) || file.getName().endsWith(".json")) {
            return extractJsonDocument(downloadResult.getCountry(), source, downloadHistory, file, downloadResult.getContent());
        } else {
            return extractHtmlDocument(downloadResult.getCountry(), source, downloadHistory, file, downloadResult.getContent());
        }
    }

    private ExtractionResult extractHtmlDocument(
            String country,
            RegulationSourceEntity source,
            RegulationDownloadHistoryEntity downloadHistory,
            File file,
            byte[] rawBytes) {

        long recordsInserted = 0;
        int sectionsFound = 0;

        try {
            String html = rawBytes != null ? new String(rawBytes, StandardCharsets.UTF_8) : Jsoup.parse(file, "UTF-8").html();
            Document doc = Jsoup.parse(html);
            doc.select("header, footer, nav, script, style, iframe, noscript, .cookie-banner, .navigation").remove();

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
                            .country(country)
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
            }

            log.info("Extracted HTML text for {}. Sections: {}, Records: {}", country, sectionsFound, recordsInserted);

            return ExtractionResult.builder()
                    .country(country)
                    .format("HTML")
                    .sectionsExtracted(sectionsFound)
                    .pagesExtracted(1)
                    .recordsInserted(recordsInserted)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Error extracting HTML document for {}", country, e);
            return ExtractionResult.builder().country(country).format("HTML").recordsInserted(0).success(false).build();
        }
    }

    private ExtractionResult extractJsonDocument(
            String country,
            RegulationSourceEntity source,
            RegulationDownloadHistoryEntity downloadHistory,
            File file,
            byte[] rawBytes) {

        long recordsInserted = 0;
        int itemsFound = 0;

        try {
            JsonNode rootNode = rawBytes != null ? objectMapper.readTree(rawBytes) : objectMapper.readTree(file);
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
                            .country(country)
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

            log.info("Extracted JSON text for {}. Items: {}, Records: {}", country, itemsFound, recordsInserted);

            return ExtractionResult.builder()
                    .country(country)
                    .format("JSON")
                    .sectionsExtracted(itemsFound)
                    .pagesExtracted(1)
                    .recordsInserted(recordsInserted)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Error extracting JSON document for {}", country, e);
            return ExtractionResult.builder().country(country).format("JSON").recordsInserted(0).success(false).build();
        }
    }

    private ExtractionResult extractPdfDocument(
            String country,
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
                            .country(country)
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
            }

            log.info("Extracted PDF text for {}. Pages: {}, Records: {}", country, totalPages, recordsInserted);

            return ExtractionResult.builder()
                    .country(country)
                    .format("PDF")
                    .sectionsExtracted(totalPages)
                    .pagesExtracted(totalPages)
                    .recordsInserted(recordsInserted)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Error extracting PDF document for {}", country, e);
            return ExtractionResult.builder().country(country).format("PDF").recordsInserted(0).success(false).build();
        }
    }
}
