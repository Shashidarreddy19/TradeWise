package com.cbec.ai.pipeline.service;

import com.cbec.ai.pipeline.model.entity.DownloadHistoryEntity;
import com.cbec.ai.pipeline.model.entity.HsRawEntity;
import com.cbec.ai.pipeline.repository.DownloadHistoryRepository;
import com.cbec.ai.pipeline.repository.HsRawRepository;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.springframework.stereotype.Service;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class SouthKoreaExcelExtractorService {

    private final HsRawRepository hsRawRepository;
    private final DownloadHistoryRepository downloadHistoryRepository;

    public SouthKoreaExcelExtractorService(
            HsRawRepository hsRawRepository,
            DownloadHistoryRepository downloadHistoryRepository) {
        this.hsRawRepository = hsRawRepository;
        this.downloadHistoryRepository = downloadHistoryRepository;
    }

    @Data
    @Builder
    public static class KoreaExcelExtractionResult {
        private String classificationFileName;
        private String tariffFileName;
        private long classificationRowsRead;
        private long tariffRowsRead;
        private long mergedRowsExtracted;
        private long totalInsertedHsRaw;
        private String datasetVersion;
    }

    @Data
    @Builder
    public static class KoreaClassificationItem {
        private String hsCode;
        private String koreanDescription;
        private String englishDescription;
        private String quantityUnit;
        private String weightUnit;
        private String exportPropertyCode;
        private String importPropertyCode;
        private String classificationCode;
        private String classificationName;
        private String startDate;
        private String endDate;
    }

    @Data
    @Builder
    public static class KoreaTariffRateItem {
        private String hsCode;
        private String tariffRateType;
        private String tariffRate;
        private String taxPerUnit;
        private String referencePrice;
        private String applicableCountryGroup;
        private String usageTariffType;
        private String effectiveStartDate;
        private String effectiveExpiryDate;
    }

    /**
     * Extracts South Korea Customs Tariff dataset by streaming and joining both official Excel workbooks into `hs_raw`.
     */
    public KoreaExcelExtractionResult extractSouthKoreaTariffData(
            String classificationPath, String tariffPath, Long executionId, String datasetVersion) {
        long startTime = System.currentTimeMillis();
        String classFileStr = classificationPath != null ? classificationPath : "관세청_HS부호_20260101.xlsx";
        String tariffFileStr = tariffPath != null ? tariffPath : "관세청_품목번호별 관세율표_20260211.xlsx";
        String version = datasetVersion != null ? datasetVersion : "HSK_2026";

        File classFile = new File(classFileStr);
        File tariffFile = new File(tariffFileStr);

        if (!classFile.exists() || !tariffFile.exists()) {
            log.error("South Korea Excel files not found. Class: {}, Tariff: {}", classFile.getAbsolutePath(), tariffFile.getAbsolutePath());
            throw new IllegalArgumentException("South Korea official Excel files not found at specified paths.");
        }

        log.info("Starting South Korea ETL Extraction. File 1: {}, File 2: {}", classFile.getName(), tariffFile.getName());

        // Archive both original files
        archiveFile(classFile, version);
        archiveFile(tariffFile, version);

        // 1. Read File 1 (HS Classification) into In-Memory Map
        Map<String, KoreaClassificationItem> classificationMap = new HashMap<>();
        long classRowsRead = 0;

        try (Workbook wb = WorkbookFactory.create(classFile)) {
            Sheet sheet = wb.getSheetAt(0);
            for (Row r : sheet) {
                if (r.getRowNum() == 0) continue; // Skip header
                classRowsRead++;

                String rawCode = getCellValue(r.getCell(0));
                String cleanCode = normalizeHsCode(rawCode);
                if (cleanCode.isEmpty()) continue;

                KoreaClassificationItem item = KoreaClassificationItem.builder()
                        .hsCode(cleanCode)
                        .startDate(getCellValue(r.getCell(1)))
                        .endDate(getCellValue(r.getCell(2)))
                        .koreanDescription(getCellValue(r.getCell(3)))
                        .englishDescription(getCellValue(r.getCell(4)))
                        .quantityUnit(getCellValue(r.getCell(9)))
                        .weightUnit(getCellValue(r.getCell(10)))
                        .exportPropertyCode(getCellValue(r.getCell(11)))
                        .importPropertyCode(getCellValue(r.getCell(12)))
                        .classificationCode(getCellValue(r.getCell(18)))
                        .classificationName(getCellValue(r.getCell(19)))
                        .build();

                classificationMap.put(cleanCode, item);
            }
        } catch (Exception e) {
            log.error("Error reading South Korea Classification Excel File 1", e);
            throw new RuntimeException("Failed reading South Korea Classification file: " + e.getMessage(), e);
        }

        log.info("Read {} classification records into memory from File 1", classificationMap.size());

        // 2. Stream File 2 (Tariff Rates) using SAX Event Parser to avoid OOM
        Map<String, KoreaTariffRateItem> tariffMap = new HashMap<>();
        long[] tariffRowsCounter = new long[1];

        try (OPCPackage pkg = OPCPackage.open(tariffFile)) {
            XSSFReader reader = new XSSFReader(pkg);
            SharedStringsTable sst = (SharedStringsTable) reader.getSharedStringsTable();

            XMLReader parser = XMLReaderFactory.createXMLReader();
            parser.setContentHandler(new DefaultHandler() {
                private String lastContents = "";
                private boolean isString = false;
                private int rowNum = 0;
                private List<String> cellValues = new ArrayList<>();

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) {
                    if ("row".equals(qName)) {
                        rowNum++;
                        cellValues.clear();
                    }
                    if ("c".equals(qName)) {
                        String cellType = attributes.getValue("t");
                        isString = "s".equals(cellType);
                    }
                    lastContents = "";
                }

                @Override
                public void characters(char[] ch, int start, int length) {
                    lastContents += new String(ch, start, length);
                }

                @Override
                public void endElement(String uri, String localName, String qName) {
                    if (isString && !lastContents.isEmpty()) {
                        try {
                            int idx = Integer.parseInt(lastContents);
                            lastContents = sst.getItemAt(idx).getString();
                        } catch (Exception ignored) {}
                    }
                    if ("v".equals(qName) || "t".equals(qName)) {
                        cellValues.add(lastContents.trim());
                    }
                    if ("row".equals(qName)) {
                        if (rowNum > 1 && !cellValues.isEmpty()) {
                            tariffRowsCounter[0]++;
                            String rawCode = cellValues.size() > 0 ? cellValues.get(0) : "";
                            String cleanCode = normalizeHsCode(rawCode);

                            if (!cleanCode.isEmpty() && !tariffMap.containsKey(cleanCode)) {
                                KoreaTariffRateItem rateItem = KoreaTariffRateItem.builder()
                                        .hsCode(cleanCode)
                                        .tariffRateType(cellValues.size() > 1 ? cellValues.get(1) : "")
                                        .tariffRate(cellValues.size() > 2 ? cellValues.get(2) : "")
                                        .taxPerUnit(cellValues.size() > 3 ? cellValues.get(3) : "")
                                        .referencePrice(cellValues.size() > 4 ? cellValues.get(4) : "")
                                        .applicableCountryGroup(cellValues.size() > 5 ? cellValues.get(5) : "")
                                        .usageTariffType(cellValues.size() > 6 ? cellValues.get(6) : "")
                                        .effectiveStartDate(cellValues.size() > 7 ? cellValues.get(7) : "")
                                        .effectiveExpiryDate(cellValues.size() > 8 ? cellValues.get(8) : "")
                                        .build();

                                tariffMap.put(cleanCode, rateItem);
                            }
                        }
                    }
                }
            });

            XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) reader.getSheetsData();
            if (sheets.hasNext()) {
                try (InputStream sheetStream = sheets.next()) {
                    parser.parse(new InputSource(sheetStream));
                }
            }
        } catch (Exception e) {
            log.error("Error streaming South Korea Tariff Rates Excel File 2", e);
            throw new RuntimeException("Failed streaming South Korea Tariff Rates file: " + e.getMessage(), e);
        }

        log.info("Streamed {} tariff rows from File 2. Unique tariff codes matched: {}", tariffRowsCounter[0], tariffMap.size());

        // 3. Perform In-Memory Join & Insert into hs_raw
        List<HsRawEntity> batch = new ArrayList<>();
        long totalInserted = 0;
        long totalMerged = 0;

        for (Map.Entry<String, KoreaClassificationItem> entry : classificationMap.entrySet()) {
            String hsCode = entry.getKey();
            KoreaClassificationItem cls = entry.getValue();
            KoreaTariffRateItem tr = tariffMap.get(hsCode);

            String engDesc = cls.getEnglishDescription();
            String korDesc = cls.getKoreanDescription();
            String descToUse = (engDesc != null && !engDesc.trim().isEmpty()) ? engDesc.trim() : korDesc;

            if (descToUse == null || descToUse.trim().isEmpty()) continue;

            totalMerged++;

            HsRawEntity raw = HsRawEntity.builder()
                    .executionId(executionId != null ? executionId : 1L)
                    .customsTerritory("KR")
                    .country("South Korea")
                    .rawNationalCode(hsCode)
                    .rawDescription(descToUse)
                    .unit(cls.getWeightUnit() != null ? cls.getWeightUnit() : cls.getQuantityUnit())
                    .sourceName("Korea Customs Service Official Excel")
                    .datasetVersion(version)
                    .build();

            batch.add(raw);

            if (batch.size() >= 500) {
                hsRawRepository.saveAll(batch);
                totalInserted += batch.size();
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            hsRawRepository.saveAll(batch);
            totalInserted += batch.size();
            batch.clear();
        }

        log.info("South Korea Extraction & In-Memory Join Complete. Classification Read: {}, Tariff Read: {}, Merged: {}, Inserted into hs_raw: {}, Time: {} ms",
                classRowsRead, tariffRowsCounter[0], totalMerged, totalInserted, (System.currentTimeMillis() - startTime));

        return KoreaExcelExtractionResult.builder()
                .classificationFileName(classFile.getName())
                .tariffFileName(tariffFile.getName())
                .classificationRowsRead(classRowsRead)
                .tariffRowsRead(tariffRowsCounter[0])
                .mergedRowsExtracted(totalMerged)
                .totalInsertedHsRaw(totalInserted)
                .datasetVersion(version)
                .build();
    }

    private String normalizeHsCode(String raw) {
        if (raw == null) return "";
        String clean = raw.replaceAll("[^0-9]", "");
        if (clean.length() == 9) {
            clean = "0" + clean;
        }
        return clean;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private void archiveFile(File file, String version) {
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String sha256 = computeSha256(bytes);
            LocalDateTime now = LocalDateTime.now();
            String yearStr = now.format(DateTimeFormatter.ofPattern("yyyy"));
            String monthStr = now.format(DateTimeFormatter.ofPattern("MM"));

            Path storageDir = Paths.get("downloads", "SouthKorea", yearStr, monthStr);
            Files.createDirectories(storageDir);

            Path targetFile = storageDir.resolve(file.getName().replace(".xlsx", "") + "_" + System.currentTimeMillis() + ".xlsx");
            Files.write(targetFile, bytes);

            DownloadHistoryEntity history = DownloadHistoryEntity.builder()
                    .country("South Korea")
                    .sourceName("Korea Customs Service Official Excel")
                    .fileName(targetFile.getFileName().toString())
                    .filePath(targetFile.toAbsolutePath().toString())
                    .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .fileSize((long) bytes.length)
                    .sha256(sha256)
                    .downloadUrl("local://" + file.getName())
                    .httpStatus(200)
                    .datasetVersion(version)
                    .build();

            downloadHistoryRepository.save(history);
            log.info("Archived South Korea Excel File {} to {} (SHA-256: {})", file.getName(), targetFile.toAbsolutePath(), sha256);
        } catch (Exception e) {
            log.warn("Could not archive South Korea Excel file {}: {}", file.getName(), e.getMessage());
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
