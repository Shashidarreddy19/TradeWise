package com.cbec.ai.pipeline.service;

import com.cbec.ai.pipeline.model.entity.HsRawEntity;
import com.cbec.ai.pipeline.repository.HsRawRepository;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class JapanHtmlExtractorService {

    private final HsRawRepository hsRawRepository;

    public JapanHtmlExtractorService(HsRawRepository hsRawRepository) {
        this.hsRawRepository = hsRawRepository;
    }

    @Data
    @Builder
    public static class JapanHtmlExtractionResult {
        private long rowsExtracted;
        private long rowsInserted;
        private long rowsSkipped;
    }

    /**
     * Parses Jsoup HTML Document of a Japan Customs Tariff Chapter page and inserts tariff records into `hs_raw`.
     */
    public JapanHtmlExtractionResult extractJapanTariffFromHtml(Document doc, String chapterNumber, Long executionId, String datasetVersion) {
        String version = datasetVersion != null ? datasetVersion : "JAPAN_TARIFF_2026";
        if (doc == null) {
            return JapanHtmlExtractionResult.builder().rowsExtracted(0).rowsInserted(0).rowsSkipped(0).build();
        }

        List<HsRawEntity> batch = new ArrayList<>();
        long rowsExtracted = 0;
        long rowsInserted = 0;
        long rowsSkipped = 0;

        Elements rows = doc.select("tr");

        for (Element row : rows) {
            Element cstCell = row.selectFirst("td.shell_var1_CSTNO, td[class*=CSTNO]");
            Element hscodeCell = row.selectFirst("td.shell_var1_HSCODE, td[class*=HSCODE]");
            Element itemCell = row.selectFirst("td.shell_var1_ITEM_NAME, td[class*=ITEM_NAME]");

            if (cstCell == null && itemCell == null) {
                rowsSkipped++;
                continue;
            }

            String cstText = cstCell != null ? cstCell.text().trim() : "";
            String hsCodeSuffix = hscodeCell != null ? hscodeCell.text().trim() : "";
            String itemText = itemCell != null ? itemCell.text().trim() : "";

            if (itemText.isEmpty()) {
                rowsSkipped++;
                continue;
            }

            String cleanBase = cstText.replaceAll("[^0-9]", "");
            String cleanSuffix = hsCodeSuffix.replaceAll("[^0-9]", "");

            String fullCode = cleanBase;
            if (!cleanSuffix.isEmpty() && cleanBase.length() == 6) {
                fullCode = cleanBase + cleanSuffix; // 9-digit Japan national code (e.g. 030119 + 000 = 030119000)
            } else if (!cleanSuffix.isEmpty() && cleanBase.length() == 4) {
                fullCode = cleanBase + cleanSuffix; // e.g. 0301 + 000 = 0301000
            }

            if (fullCode.length() < 6) {
                rowsSkipped++;
                continue;
            }

            // Standardize 6, 8, 9, 10-digit codes
            if (fullCode.length() == 7) fullCode = "0" + fullCode;
            else if (fullCode.length() == 9 && !fullCode.startsWith("0") && chapterNumber.length() == 2 && chapterNumber.startsWith("0")) {
                // Ensure leading zero preserved if chapter is 01-09
                if (!fullCode.startsWith(chapterNumber)) {
                    fullCode = chapterNumber.substring(0, 1) + fullCode;
                }
            }

            if (fullCode.length() < 8 || fullCode.length() > 10) {
                rowsSkipped++;
                continue;
            }

            rowsExtracted++;

            HsRawEntity raw = HsRawEntity.builder()
                    .executionId(executionId != null ? executionId : 1L)
                    .customsTerritory("JP")
                    .country("Japan")
                    .rawNationalCode(fullCode)
                    .rawDescription(itemText)
                    .sourceName("Japan Customs Tariff")
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

        return JapanHtmlExtractionResult.builder()
                .rowsExtracted(rowsExtracted)
                .rowsInserted(rowsInserted)
                .rowsSkipped(rowsSkipped)
                .build();
    }
}
