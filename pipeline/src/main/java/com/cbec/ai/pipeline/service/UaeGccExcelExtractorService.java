package com.cbec.ai.pipeline.service;

import com.cbec.ai.pipeline.model.entity.HsRawEntity;
import com.cbec.ai.pipeline.repository.HsRawRepository;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class UaeGccExcelExtractorService {

    private final HsRawRepository hsRawRepository;
    private final DataFormatter dataFormatter;

    public UaeGccExcelExtractorService(HsRawRepository hsRawRepository) {
        this.hsRawRepository = hsRawRepository;
        this.dataFormatter = new DataFormatter();
    }

    @Data
    @Builder
    public static class UaeExcelExtractionResult {
        private String workbookName;
        private String activeSheetName;
        private int totalWorksheets;
        private long rowsScanned;
        private long rowsExtracted;
        private long rowsInserted;
        private long rowsSkipped;
        private String datasetVersion;
    }

    /**
     * Low-memory Apache POI reader extracting 13,450+ official UAE GCC customs tariff lines into `hs_raw`.
     */
    public UaeExcelExtractionResult extractUaeGccExcelData(String excelPath, Long executionId, String datasetVersion) {
        long startTime = System.currentTimeMillis();
        String filePath = excelPath != null ? excelPath : "HSCodeMaster-v3.3customers.xlsx";
        String version = datasetVersion != null ? datasetVersion : "GCC_TARIFF_v3.3";

        File file = new File(filePath);
        if (!file.exists()) {
            log.error("UAE GCC Excel file not found at: {}", filePath);
            throw new IllegalArgumentException("UAE GCC Excel file not found at: " + filePath);
        }

        log.info("Starting POI Excel extraction for UAE GCC Customs Tariff from: {}", file.getAbsolutePath());

        List<HsRawEntity> batch = new ArrayList<>();
        long rowsScanned = 0;
        long rowsExtracted = 0;
        long rowsInserted = 0;
        long rowsSkipped = 0;
        String activeSheetName = "HSCodeMaster";
        int totalWorksheets = 0;

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {

            totalWorksheets = workbook.getNumberOfSheets();
            Sheet targetSheet = null;

            // Automatically detect sheet containing tariff data
            for (int i = 0; i < totalWorksheets; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                if (sheet.getPhysicalNumberOfRows() > 0) {
                    Row firstRow = sheet.getRow(sheet.getFirstRowNum());
                    if (firstRow != null) {
                        for (Cell cell : firstRow) {
                            String cellVal = cell.toString().trim();
                            if ("NewHSCode".equalsIgnoreCase(cellVal) || "LongDescEn".equalsIgnoreCase(cellVal)) {
                                targetSheet = sheet;
                                activeSheetName = sheet.getSheetName();
                                break;
                            }
                        }
                    }
                }
                if (targetSheet != null) break;
            }

            if (targetSheet == null) {
                targetSheet = workbook.getSheetAt(0);
                activeSheetName = targetSheet.getSheetName();
            }

            log.info("Processing worksheet: '{}' with {} physical rows", activeSheetName, targetSheet.getPhysicalNumberOfRows());

            int newHsCodeColIdx = 1;
            int longDescEnColIdx = 3;
            int unitColIdx = 4;

            Row headerRow = targetSheet.getRow(targetSheet.getFirstRowNum());
            if (headerRow != null) {
                for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                    Cell cell = headerRow.getCell(c);
                    if (cell != null) {
                        String colName = cell.toString().trim();
                        if ("NewHSCode".equalsIgnoreCase(colName)) newHsCodeColIdx = c;
                        else if ("LongDescEn".equalsIgnoreCase(colName)) longDescEnColIdx = c;
                        else if ("StatisticalQtyUnit".equalsIgnoreCase(colName)) unitColIdx = c;
                    }
                }
            }

            for (Row row : targetSheet) {
                if (row.getRowNum() == targetSheet.getFirstRowNum()) {
                    continue; // Skip header row
                }
                rowsScanned++;

                Cell codeCell = row.getCell(newHsCodeColIdx);
                Cell descCell = row.getCell(longDescEnColIdx);
                Cell unitCell = row.getCell(unitColIdx);

                if (codeCell == null && descCell == null) {
                    rowsSkipped++;
                    continue;
                }

                String rawCode = getCellValueAsString(codeCell);
                String rawDesc = getCellValueAsString(descCell);
                String rawUnit = getCellValueAsString(unitCell);

                if (rawCode == null || rawCode.trim().isEmpty() || rawDesc == null || rawDesc.trim().isEmpty()) {
                    rowsSkipped++;
                    continue;
                }

                String cleanCode = rawCode.replaceAll("[^0-9]", "").trim();

                // If code lost leading zero e.g. "1012100" (7 digits) -> format to "01012100" (8 digits)
                if (cleanCode.length() == 7 || cleanCode.length() == 9 || cleanCode.length() == 11) {
                    cleanCode = "0" + cleanCode;
                }

                if (cleanCode.length() < 8 || cleanCode.length() > 12) {
                    log.warn("Skipping row {}: Invalid GCC HS code length (must be 8-12 digits): {}", row.getRowNum(), rawCode);
                    rowsSkipped++;
                    continue;
                }

                rowsExtracted++;

                HsRawEntity raw = HsRawEntity.builder()
                        .executionId(executionId != null ? executionId : 1L)
                        .customsTerritory("GCC")
                        .country("United Arab Emirates")
                        .rawNationalCode(cleanCode)
                        .rawDescription(rawDesc.trim())
                        .unit(rawUnit != null && !rawUnit.trim().isEmpty() ? rawUnit.trim() : null)
                        .sourceName("UAE GCC Customs Tariff Excel")
                        .datasetVersion(version)
                        .build();

                batch.add(raw);

                if (batch.size() >= 500) {
                    hsRawRepository.saveAll(batch);
                    rowsInserted += batch.size();
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                hsRawRepository.saveAll(batch);
                rowsInserted += batch.size();
                batch.clear();
            }

            log.info("POI Excel Extraction Complete for UAE GCC. Scanned: {}, Extracted: {}, Inserted into hs_raw: {}, Skipped: {}, Time: {} ms",
                    rowsScanned, rowsExtracted, rowsInserted, rowsSkipped, (System.currentTimeMillis() - startTime));

            return UaeExcelExtractionResult.builder()
                    .workbookName(file.getName())
                    .activeSheetName(activeSheetName)
                    .totalWorksheets(totalWorksheets)
                    .rowsScanned(rowsScanned)
                    .rowsExtracted(rowsExtracted)
                    .rowsInserted(rowsInserted)
                    .rowsSkipped(rowsSkipped)
                    .datasetVersion(version)
                    .build();

        } catch (Exception e) {
            log.error("Fatal error during POI Excel extraction of UAE file {}", filePath, e);
            throw new RuntimeException("Failed extracting UAE GCC Excel file: " + e.getMessage(), e);
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        return dataFormatter.formatCellValue(cell).trim();
    }
}
