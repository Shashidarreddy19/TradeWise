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
public class AustraliaHtmlExtractorService {

    private final HsRawRepository hsRawRepository;

    public AustraliaHtmlExtractorService(HsRawRepository hsRawRepository) {
        this.hsRawRepository = hsRawRepository;
    }

    @Data
    @Builder
    public static class HtmlExtractionResult {
        private long rowsExtracted;
        private long rowsInserted;
        private long rowsSkipped;
    }

    /**
     * Parses Jsoup HTML Document of an Australian Border Force Chapter page and inserts tariff records into `hs_raw`.
     */
    public HtmlExtractionResult extractTariffRecordsFromHtml(Document doc, String chapterNumber, Long executionId, String datasetVersion) {
        String version = datasetVersion != null ? datasetVersion : "AU_TARIFF_2026";
        if (doc == null) {
            return HtmlExtractionResult.builder().rowsExtracted(0).rowsInserted(0).rowsSkipped(0).build();
        }

        List<HsRawEntity> batch = new ArrayList<>();
        long rowsExtracted = 0;
        long rowsInserted = 0;
        long rowsSkipped = 0;

        Elements rows = doc.select("table tbody tr");
        if (rows.isEmpty()) {
            rows = doc.select("tr");
        }

        String currentHeading = null;

        for (Element row : rows) {
            Elements ths = row.select("th");
            Elements tds = row.select("td");

            String refNumStr = !ths.isEmpty() ? ths.first().text().trim() : (!tds.isEmpty() ? tds.first().text().trim() : null);

            Element tcnLink = row.selectFirst("a[data-tcn]");
            String dataTcn = tcnLink != null ? tcnLink.attr("data-tcn").trim() : null;

            String statCode = "";
            String unitStr = "";
            String goodsStr = "";
            String rateStr = "";

            if (tds.size() >= 4) {
                statCode = tds.get(0).text().trim();
                unitStr = tds.get(1).text().trim();
                goodsStr = tds.get(2).text().trim();
                rateStr = tds.get(3).text().trim();
            } else if (tds.size() >= 3) {
                goodsStr = tds.get(0).text().trim();
                rateStr = tds.size() > 1 ? tds.get(1).text().trim() : "";
            }

            if (refNumStr != null && refNumStr.length() == 4 && refNumStr.matches("^\\d{4}$")) {
                currentHeading = refNumStr;
            }

            String cleanBaseCode = "";
            if (dataTcn != null && !dataTcn.isEmpty()) {
                cleanBaseCode = dataTcn.replaceAll("[^0-9]", "");
            } else if (refNumStr != null) {
                cleanBaseCode = refNumStr.replaceAll("[^0-9]", "");
            }

            String cleanStatCode = statCode.replaceAll("[^0-9]", "");
            String fullCode = cleanBaseCode;

            if (cleanStatCode.length() == 2 && cleanBaseCode.length() == 8) {
                fullCode = cleanBaseCode + cleanStatCode; // 10-digit code
            }

            if (fullCode.isEmpty() || goodsStr.isEmpty()) {
                rowsSkipped++;
                continue;
            }

            // Pad 7 to 8 digits if leading zero missing
            if (fullCode.length() == 7) {
                fullCode = "0" + fullCode;
            } else if (fullCode.length() == 9) {
                fullCode = "0" + fullCode;
            }

            if (fullCode.length() < 8 || fullCode.length() > 10) {
                rowsSkipped++;
                continue;
            }

            rowsExtracted++;

            HsRawEntity raw = HsRawEntity.builder()
                    .executionId(executionId != null ? executionId : 1L)
                    .customsTerritory("AU")
                    .country("Australia")
                    .rawNationalCode(fullCode)
                    .rawDescription(goodsStr)
                    .unit(!unitStr.isEmpty() ? unitStr : null)
                    .sourceName("Australian Border Force Schedule 3")
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

        return HtmlExtractionResult.builder()
                .rowsExtracted(rowsExtracted)
                .rowsInserted(rowsInserted)
                .rowsSkipped(rowsSkipped)
                .build();
    }
}
