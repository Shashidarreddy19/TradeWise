package com.cbec.ai.pipeline.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NormalizedHsRecordDto {
    private String customsTerritory;
    private String country;
    private String chapter;
    private String heading;
    private String hs6;
    private String nationalCode;
    private int codeLength;
    private String nomenclatureType;
    private String category;
    private String officialDescription;
    private String unit;
    private String source;
    private String sourceUrl;
    private String version;
    private LocalDateTime lastVerified;
}
