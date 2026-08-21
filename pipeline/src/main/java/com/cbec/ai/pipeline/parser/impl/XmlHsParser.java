package com.cbec.ai.pipeline.parser.impl;

import com.cbec.ai.pipeline.exception.HsPipelineException;
import com.cbec.ai.pipeline.model.dto.RawHsRecordDto;
import com.cbec.ai.pipeline.model.dto.SourceMetadataDto;
import com.cbec.ai.pipeline.model.enums.FileTypeEnum;
import com.cbec.ai.pipeline.parser.HsParser;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class XmlHsParser implements HsParser {

    private final XmlMapper xmlMapper = new XmlMapper();

    @Override
    public boolean supports(FileTypeEnum fileType) {
        return fileType == FileTypeEnum.XML;
    }

    @Override
    public List<RawHsRecordDto> parse(InputStream inputStream, SourceMetadataDto metadata) {
        List<RawHsRecordDto> rawRecords = new ArrayList<>();
        try {
            JsonNode root = xmlMapper.readTree(inputStream);
            traverseXmlNodes(root, metadata, rawRecords);
        } catch (Exception e) {
            log.error("Failed to parse XML file for source {}", metadata.getSourceName(), e);
            throw new HsPipelineException("XML parsing failed: " + e.getMessage(), e);
        }
        return rawRecords;
    }

    private void traverseXmlNodes(JsonNode node, SourceMetadataDto metadata, List<RawHsRecordDto> records) {
        if (node.isObject()) {
            String hsCode = extractField(node, "hsCode", "hs_code", "code", "goodsCode", "tariffCode", "goods_code");
            String desc = extractField(node, "description", "official_description", "desc", "goodsDescription");
            String unit = extractField(node, "unit", "uom");

            if (hsCode != null && !hsCode.trim().isEmpty() && desc != null && !desc.trim().isEmpty()) {
                records.add(RawHsRecordDto.builder()
                        .country(metadata.getCountry())
                        .rawHsCode(hsCode)
                        .rawDescription(desc)
                        .unit(unit != null ? unit : "")
                        .source(metadata.getSourceName())
                        .sourceUrl(metadata.getSourceUrlOrPath())
                        .version(metadata.getVersion())
                        .build());
            }

            node.fields().forEachRemaining(entry -> traverseXmlNodes(entry.getValue(), metadata, records));
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                traverseXmlNodes(child, metadata, records);
            }
        }
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
