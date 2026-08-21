package com.cbec.ai.pipeline.stage;

import com.cbec.ai.pipeline.model.dto.NormalizedHsRecordDto;
import com.cbec.ai.pipeline.model.dto.RawHsRecordDto;
import com.cbec.ai.pipeline.model.dto.SourceMetadataDto;
import com.cbec.ai.pipeline.util.HsCodeCleaner;
import com.cbec.ai.pipeline.util.StringNormalizationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class Stage3NormalizationService {

    /**
     * Stage 3: Normalization.
     * Extracts HS hierarchy (Chapter=2d, Heading=4d, HS6=6d) and national_code without altering official text verbatim.
     */
    public List<NormalizedHsRecordDto> normalize(List<RawHsRecordDto> rawRecords, SourceMetadataDto metadata) {
        log.info("Stage 3 [Normalization] - Normalizing {} raw records for country: {} (Territory: {})",
                rawRecords.size(), metadata.getCountry(), metadata.getCustomsTerritory());

        List<NormalizedHsRecordDto> normalizedList = new ArrayList<>();

        for (RawHsRecordDto raw : rawRecords) {
            String digits = HsCodeCleaner.cleanDigits(raw.getRawHsCode());
            String officialDesc = StringNormalizationUtils.normalizeDescription(raw.getRawDescription());
            String country = StringNormalizationUtils.sanitizeField(raw.getCountry());
            String territory = (raw.getCustomsTerritory() != null && !raw.getCustomsTerritory().isEmpty())
                    ? raw.getCustomsTerritory() : metadata.getCustomsTerritory();
            String unit = StringNormalizationUtils.sanitizeField(raw.getUnit());
            String source = StringNormalizationUtils.sanitizeField(raw.getSource());
            String sourceUrl = StringNormalizationUtils.sanitizeField(raw.getSourceUrl());
            String version = StringNormalizationUtils.sanitizeField(raw.getVersion());

            String chapter = digits.length() >= 2 ? digits.substring(0, 2) : "";
            String heading = digits.length() >= 4 ? digits.substring(0, 4) : "";
            String hs6 = digits.length() >= 6 ? digits.substring(0, 6) : "";
            String nationalCode = digits;
            int codeLength = digits.length();

            NormalizedHsRecordDto dto = NormalizedHsRecordDto.builder()
                    .customsTerritory(territory)
                    .country(country)
                    .chapter(chapter)
                    .heading(heading)
                    .hs6(hs6)
                    .nationalCode(nationalCode)
                    .codeLength(codeLength)
                    .nomenclatureType(metadata.getNomenclatureType())
                    .officialDescription(officialDesc)
                    .unit(unit)
                    .source(source)
                    .sourceUrl(sourceUrl)
                    .version(version)
                    .lastVerified(LocalDateTime.now())
                    .build();

            normalizedList.add(dto);
        }

        log.info("Stage 3 [Normalization] - Normalized {} records successfully", normalizedList.size());
        return normalizedList;
    }
}
