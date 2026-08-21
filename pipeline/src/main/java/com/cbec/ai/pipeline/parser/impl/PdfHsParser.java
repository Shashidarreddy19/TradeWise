package com.cbec.ai.pipeline.parser.impl;

import com.cbec.ai.pipeline.exception.HsPipelineException;
import com.cbec.ai.pipeline.model.dto.RawHsRecordDto;
import com.cbec.ai.pipeline.model.dto.SourceMetadataDto;
import com.cbec.ai.pipeline.model.enums.FileTypeEnum;
import com.cbec.ai.pipeline.parser.HsParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class PdfHsParser implements HsParser {

    // Matches HS code at the start of a line (e.g. "2710.19.20", "27101920", "0901.11") followed by whitespace and description
    private static final Pattern PDF_LINE_PATTERN = Pattern.compile("^(\\d{4}\\.?[0-9]{2}\\.?[0-9]{2}|\\d{8})\\s+(.+)$");

    @Override
    public boolean supports(FileTypeEnum fileType) {
        return fileType == FileTypeEnum.PDF;
    }

    @Override
    public List<RawHsRecordDto> parse(InputStream inputStream, SourceMetadataDto metadata) {
        List<RawHsRecordDto> rawRecords = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);

            String[] lines = text.split("\\r?\\n");
            for (String line : lines) {
                String trimmed = line.trim();
                // Filter headers, footers, page numbers
                if (trimmed.isEmpty() || trimmed.toLowerCase().startsWith("page ") || trimmed.toLowerCase().contains("customs tariff")) {
                    continue;
                }

                Matcher matcher = PDF_LINE_PATTERN.matcher(trimmed);
                if (matcher.find()) {
                    String rawHs = matcher.group(1);
                    String rawDesc = matcher.group(2);
                    rawRecords.add(RawHsRecordDto.builder()
                            .country(metadata.getCountry())
                            .rawHsCode(rawHs)
                            .rawDescription(rawDesc)
                            .unit("")
                            .source(metadata.getSourceName())
                            .sourceUrl(metadata.getSourceUrlOrPath())
                            .version(metadata.getVersion())
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse PDF document for source {}", metadata.getSourceName(), e);
            throw new HsPipelineException("PDF parsing failed: " + e.getMessage(), e);
        }
        return rawRecords;
    }
}
