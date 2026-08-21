package com.cbec.ai.pipeline.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RawHsRecordDto {
    private String customsTerritory;
    private String country;
    private String rawHsCode;
    private String rawDescription;
    private String unit;
    private String source;
    private String sourceUrl;
    private String version;
}
