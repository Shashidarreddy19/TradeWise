package com.cbec.ai.pipeline.parser;

import com.cbec.ai.pipeline.exception.HsPipelineException;
import com.cbec.ai.pipeline.model.enums.FileTypeEnum;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HsParserFactory {

    private final List<HsParser> parsers;

    public HsParserFactory(List<HsParser> parsers) {
        this.parsers = parsers;
    }

    public HsParser getParser(FileTypeEnum fileType) {
        return parsers.stream()
                .filter(p -> p.supports(fileType))
                .findFirst()
                .orElseThrow(() -> new HsPipelineException("Unsupported file type parser for: " + fileType));
    }
}
