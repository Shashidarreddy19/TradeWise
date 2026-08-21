package com.cbec.ai.pipeline.stage;

import com.cbec.ai.pipeline.model.dto.NormalizedHsRecordDto;
import com.cbec.ai.pipeline.model.dto.RawHsRecordDto;
import com.cbec.ai.pipeline.model.dto.SourceMetadataDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NormalizationServiceTest {

    private Stage3NormalizationService normalizationService;

    @BeforeEach
    void setUp() {
        normalizationService = new Stage3NormalizationService();
    }

    @Test
    void testNormalizeRecordPreservesOfficialTextExactness() {
        RawHsRecordDto raw = RawHsRecordDto.builder()
                .country("India")
                .rawHsCode("2710.19.20")
                .rawDescription("   Motor  Spirit   (Gasoline)   including  Aviation   Spirit   ")
                .unit(" LTR ")
                .source("DGFT")
                .sourceUrl("http://dgft.gov.in")
                .version("2026.1")
                .build();

        SourceMetadataDto metadata = SourceMetadataDto.builder()
                .customsTerritory("IN")
                .country("India")
                .nomenclatureType("ITC_HS")
                .expectedCodeLength(8)
                .build();

        List<NormalizedHsRecordDto> result = normalizationService.normalize(List.of(raw), metadata);

        assertEquals(1, result.size());
        NormalizedHsRecordDto norm = result.get(0);
        assertEquals("IN", norm.getCustomsTerritory());
        assertEquals("India", norm.getCountry());
        assertEquals("27", norm.getChapter());
        assertEquals("2710", norm.getHeading());
        assertEquals("271019", norm.getHs6());
        assertEquals("27101920", norm.getNationalCode());
        assertEquals(8, norm.getCodeLength());
        assertEquals("ITC_HS", norm.getNomenclatureType());
        assertEquals("Motor Spirit (Gasoline) including Aviation Spirit", norm.getOfficialDescription());
        assertEquals("LTR", norm.getUnit());
    }
}
