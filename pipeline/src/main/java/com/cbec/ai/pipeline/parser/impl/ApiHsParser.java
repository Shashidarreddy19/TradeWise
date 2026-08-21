package com.cbec.ai.pipeline.parser.impl;

import com.cbec.ai.pipeline.model.dto.RawHsRecordDto;
import com.cbec.ai.pipeline.model.dto.SourceMetadataDto;
import com.cbec.ai.pipeline.model.enums.FileTypeEnum;
import com.cbec.ai.pipeline.parser.HsParser;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
public class ApiHsParser implements HsParser {

    private final JsonHsParser jsonHsParser;

    public ApiHsParser(JsonHsParser jsonHsParser) {
        this.jsonHsParser = jsonHsParser;
    }

    @Override
    public boolean supports(FileTypeEnum fileType) {
        return fileType == FileTypeEnum.API;
    }

    @Override
    public List<RawHsRecordDto> parse(InputStream inputStream, SourceMetadataDto metadata) {
        // Official APIs return structured JSON payloads
        return jsonHsParser.parse(inputStream, metadata);
    }
}
