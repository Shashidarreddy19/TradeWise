package com.cbec.ai.pipeline.parser.impl;

import com.cbec.ai.pipeline.exception.HsPipelineException;
import com.cbec.ai.pipeline.model.dto.RawHsRecordDto;
import com.cbec.ai.pipeline.model.dto.SourceMetadataDto;
import com.cbec.ai.pipeline.model.enums.FileTypeEnum;
import com.cbec.ai.pipeline.parser.HsParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ExcelHsParser implements HsParser {

    @Override
    public boolean supports(FileTypeEnum fileType) {
        return fileType == FileTypeEnum.EXCEL;
    }

    @Override
    public List<RawHsRecordDto> parse(InputStream inputStream, SourceMetadataDto metadata) {
        List<RawHsRecordDto> rawRecords = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            int hsColIndex = 0;
            int descColIndex = 1;
            int unitColIndex = 2;
            boolean headersFound = false;

            for (Row row : sheet) {
                if (row == null) continue;

                // Detect headers on first non-empty row
                if (!headersFound) {
                    for (Cell cell : row) {
                        String val = formatter.formatCellValue(cell).toLowerCase().trim();
                        if (val.contains("hs") || val.contains("code") || val.contains("tariff")) {
                            hsColIndex = cell.getColumnIndex();
                            headersFound = true;
                        } else if (val.contains("desc") || val.contains("item") || val.contains("goods")) {
                            descColIndex = cell.getColumnIndex();
                        } else if (val.contains("unit") || val.contains("uom")) {
                            unitColIndex = cell.getColumnIndex();
                        }
                    }
                    if (headersFound) continue; // Skip header row itself
                }

                String rawHs = formatter.formatCellValue(row.getCell(hsColIndex)).trim();
                String rawDesc = formatter.formatCellValue(row.getCell(descColIndex)).trim();
                String unit = formatter.formatCellValue(row.getCell(unitColIndex)).trim();

                if (rawHs.isEmpty() && rawDesc.isEmpty()) {
                    continue; // Skip blank row
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
            log.error("Failed to parse Excel workbook for source {}", metadata.getSourceName(), e);
            throw new HsPipelineException("Excel parsing failed: " + e.getMessage(), e);
        }
        return rawRecords;
    }
}
