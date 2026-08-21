package com.cbec.ai.pipeline.parser.impl;

import com.cbec.ai.pipeline.exception.HsPipelineException;
import com.cbec.ai.pipeline.model.dto.RawHsRecordDto;
import com.cbec.ai.pipeline.model.dto.SourceMetadataDto;
import com.cbec.ai.pipeline.model.enums.FileTypeEnum;
import com.cbec.ai.pipeline.parser.HsParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class JsonHsParser implements HsParser {

    private final ObjectMapper objectMapper;

    public JsonHsParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(FileTypeEnum fileType) {
        return fileType == FileTypeEnum.JSON;
    }

    @Override
    public List<RawHsRecordDto> parse(InputStream inputStream, SourceMetadataDto metadata) {
        List<RawHsRecordDto> rawRecords = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(inputStream);
            JsonNode itemsArray = root.isArray() ? root : (root.has("items") ? root.get("items") : root.has("data") ? root.get("data") : root.get("results"));

            if (itemsArray != null && itemsArray.isArray()) {
                for (JsonNode node : itemsArray) {
                    String hsCode = extractField(node, "hsCode", "hs_code", "htsno", "code", "hs8", "tariffCode");
                    String description = extractField(node, "description", "official_description", "desc", "description_text");
                    String unit = extractField(node, "unit", "uom", "quantity_unit");

                    if (hsCode != null && !hsCode.trim().isEmpty()) {
                        rawRecords.add(RawHsRecordDto.builder()
                                .country(metadata.getCountry())
                                .rawHsCode(hsCode)
                                .rawDescription(description != null ? description : "")
                                .unit(unit != null ? unit : "")
                                .source(metadata.getSourceName())
                                .sourceUrl(metadata.getSourceUrlOrPath())
                                .version(metadata.getVersion())
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse JSON file for source {}", metadata.getSourceName(), e);
            throw new HsPipelineException("JSON parsing failed: " + e.getMessage(), e);
        }
        return rawRecords;
    }

    private String extractField(JsonNode node, String... fieldNames) {
        for (String fn : fieldNames) {
            if (node.has(fn) && !node.get(fn).isNull()) {
                return node.get(fn).asText();
            }
        }
        return null;
    }
}
