package com.cbec.ai.pipeline.parser.impl;

import com.cbec.ai.pipeline.exception.HsPipelineException;
import com.cbec.ai.pipeline.model.dto.RawHsRecordDto;
import com.cbec.ai.pipeline.model.dto.SourceMetadataDto;
import com.cbec.ai.pipeline.model.enums.FileTypeEnum;
import com.cbec.ai.pipeline.parser.HsParser;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class HtmlHsParser implements HsParser {

    @Override
    public boolean supports(FileTypeEnum fileType) {
        return fileType == FileTypeEnum.HTML;
    }

    @Override
    public List<RawHsRecordDto> parse(InputStream inputStream, SourceMetadataDto metadata) {
        List<RawHsRecordDto> rawRecords = new ArrayList<>();
        try {
            Document doc = Jsoup.parse(inputStream, StandardCharsets.UTF_8.name(), metadata.getSourceUrlOrPath() != null ? metadata.getSourceUrlOrPath() : "");

            // Ignore non-data elements: navigation, menus, ads, headers, footers, scripts, styles
            doc.select("nav, header, footer, menu, .nav, .menu, .ads, .sidebar, script, style, iframe, .header, .footer").remove();

            // Extract from tables
            Elements tables = doc.select("table");
            for (Element table : tables) {
                Elements rows = table.select("tr");
                for (Element row : rows) {
                    Elements cols = row.select("td, th");
                    if (cols.size() >= 2) {
                        String col0 = cols.get(0).text().trim();
                        String col1 = cols.get(1).text().trim();

                        // Skip table header text if col0 contains 'code' or 'hs' or 'tariff'
                        if (col0.equalsIgnoreCase("hs code") || col0.equalsIgnoreCase("code") || col0.equalsIgnoreCase("tariff code")) {
                            continue;
                        }

                        if (!col0.isEmpty() && !col1.isEmpty()) {
                            String unit = cols.size() > 2 ? cols.get(2).text().trim() : "";
                            rawRecords.add(RawHsRecordDto.builder()
                                    .country(metadata.getCountry())
                                    .rawHsCode(col0)
                                    .rawDescription(col1)
                                    .unit(unit)
                                    .source(metadata.getSourceName())
                                    .sourceUrl(metadata.getSourceUrlOrPath())
                                    .version(metadata.getVersion())
                                    .build());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse HTML document for source {}", metadata.getSourceName(), e);
            throw new HsPipelineException("HTML parsing failed: " + e.getMessage(), e);
        }
        return rawRecords;
    }
}
