package com.cbec.ai.pipeline.model.dto;

import com.cbec.ai.pipeline.model.enums.FileTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SourceMetadataDto {
    private Long sourceId;
    private String customsTerritory;
    private String country;
    private String authority;
    private String sourceName;
    private String sourceUrlOrPath;
    private String sourceType;
    private FileTypeEnum fileType;
    private String nomenclatureType;
    private int expectedCodeLength;
    private String version;
    private LocalDateTime discoveredAt;
}
