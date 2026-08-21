package com.cbec.ai.pipeline.extractor;

import com.cbec.ai.pipeline.model.dto.NormalizedHsRecordDto;
import com.cbec.ai.pipeline.model.dto.RawHsRecordDto;
import com.cbec.ai.pipeline.model.dto.SourceMetadataDto;
import com.cbec.ai.pipeline.stage.Stage3NormalizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Gcc12DigitTariffTest {

    private Stage3NormalizationService normalizationService;

    @BeforeEach
    void setUp() {
        normalizationService = new Stage3NormalizationService();
    }

    @Test
    void testGcc12DigitTariffCodePreservation() {
        RawHsRecordDto raw = RawHsRecordDto.builder()
                .customsTerritory("GCC")
                .country("Saudi Arabia")
                .rawHsCode("2710.19.20.00.00") // 12-digit GCC tariff line
                .rawDescription("Gasoline motor spirit high octane")
                .unit("LTR")
                .source("ZATCA")
                .version("2026.1")
                .build();

        SourceMetadataDto metadata = SourceMetadataDto.builder()
                .customsTerritory("GCC")
                .country("Saudi Arabia")
                .nomenclatureType("GCC_TARIFF")
                .expectedCodeLength(12)
                .build();

        List<NormalizedHsRecordDto> result = normalizationService.normalize(List.of(raw), metadata);

        assertEquals(1, result.size());
        NormalizedHsRecordDto norm = result.get(0);

        assertEquals("GCC", norm.getCustomsTerritory());
        assertEquals("27", norm.getChapter());
        assertEquals("2710", norm.getHeading());
        assertEquals("271019", norm.getHs6());
        assertEquals("271019200000", norm.getNationalCode()); // Full 12 digits retained!
        assertEquals(12, norm.getCodeLength());
        assertEquals("GCC_TARIFF", norm.getNomenclatureType());
    }
}
