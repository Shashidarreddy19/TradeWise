package com.cbec.ai.pipeline.parser.impl;

import com.cbec.ai.pipeline.exception.HsPipelineException;
import com.cbec.ai.pipeline.model.dto.RawHsRecordDto;
import com.cbec.ai.pipeline.model.dto.SourceMetadataDto;
import com.cbec.ai.pipeline.model.enums.FileTypeEnum;
import com.cbec.ai.pipeline.parser.HsParser;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class CsvHsParser implements HsParser {

    @Override
    public boolean supports(FileTypeEnum fileType) {
        return fileType == FileTypeEnum.CSV;
    }

    @Override
    public List<RawHsRecordDto> parse(InputStream inputStream, SourceMetadataDto metadata) {
        List<RawHsRecordDto> rawRecords = new ArrayList<>();
        try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).build()) {
            String[] header = reader.readNext();
            if (header == null) {
                return rawRecords;
            }

            int hsIndex = -1;
            int descIndex = -1;
            int unitIndex = -1;

            // Header auto-detection
            for (int i = 0; i < header.length; i++) {
                String h = header[i].toLowerCase().trim();
                if (h.contains("hs") || h.contains("code") || h.contains("tariff") || h.contains("hts")) {
                    if (hsIndex == -1) hsIndex = i;
                } else if (h.contains("desc") || h.contains("product") || h.contains("name") || h.contains("goods")) {
                    if (descIndex == -1) descIndex = i;
                } else if (h.contains("unit") || h.contains("uom") || h.contains("quantity")) {
                    if (unitIndex == -1) unitIndex = i;
                }
            }

            if (hsIndex == -1) hsIndex = 0;
            if (descIndex == -1) descIndex = header.length > 1 ? 1 : 0;

            String[] line;
            while ((line = reader.readNext()) != null) {
                if (line.length == 0 || (line.length == 1 && line[0].trim().isEmpty())) {
                    continue; // Skip blank rows
                }
                String rawHs = (hsIndex < line.length) ? line[hsIndex] : "";
                String rawDesc = (descIndex < line.length) ? line[descIndex] : "";
                String unit = (unitIndex != -1 && unitIndex < line.length) ? line[unitIndex] : "";

                if (rawHs.trim().isEmpty() && rawDesc.trim().isEmpty()) {
                    continue;
                }

                rawRecords.add(RawHsRecordDto.builder()
                        .country(metadata.getCountry())
                        .rawHsCode(rawHs)
                        .rawDescription(rawDesc)
                        .unit(unit)
                        .source(metadata.getSourceName())
                        .sourceUrl(metadata.getSourceUrlOrPath())
                        .version(metadata.getVersion())
                        .build());
            }
        } catch (Exception e) {
            log.error("Failed to parse CSV file for source {}", metadata.getSourceName(), e);
            throw new HsPipelineException("CSV parsing failed: " + e.getMessage(), e);
        }
        return rawRecords;
    }
}
