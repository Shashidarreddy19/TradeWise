package com.cbec.ai.pipeline.stage;

import com.cbec.ai.pipeline.model.dto.ExtractionReportDto;
import com.cbec.ai.pipeline.model.dto.NormalizedHsRecordDto;
import com.cbec.ai.pipeline.repository.RejectedRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ValidationServiceTest {

    private Stage4ValidationService validationService;
    private RejectedRecordRepository rejectedRecordRepository;

    @BeforeEach
    void setUp() {
        rejectedRecordRepository = mock(RejectedRecordRepository.class);
        when(rejectedRecordRepository.saveAll(anyList())).thenReturn(List.of());
        validationService = new Stage4ValidationService(rejectedRecordRepository);
    }

    @Test
    void testValidationPassesValidHierarchyChain() {
        ExtractionReportDto report = new ExtractionReportDto();
        NormalizedHsRecordDto record = NormalizedHsRecordDto.builder()
                .customsTerritory("IN")
                .country("India")
                .chapter("27")
                .heading("2710")
                .hs6("271019")
                .nationalCode("27101920")
                .officialDescription("Motor Spirit")
                .build();

        List<NormalizedHsRecordDto> valid = validationService.validate(List.of(record), report, 1L);

        assertEquals(1, valid.size());
        assertEquals(0, report.getTotalInvalid());
    }

    @Test
    void testValidationRejectsInvalidHeadingHierarchyChain() {
        ExtractionReportDto report = new ExtractionReportDto();
        NormalizedHsRecordDto record = NormalizedHsRecordDto.builder()
                .customsTerritory("IN")
                .country("India")
                .chapter("27")
                .heading("2810") // Heading 2810 does not start with chapter 27!
                .hs6("271019")
                .nationalCode("27101920")
                .officialDescription("Motor Spirit")
                .build();

        List<NormalizedHsRecordDto> valid = validationService.validate(List.of(record), report, 1L);

        assertEquals(0, valid.size());
        assertEquals(1, report.getTotalInvalid());
    }

    @Test
    void testValidationRejectsInvalidHs6HierarchyChain() {
        ExtractionReportDto report = new ExtractionReportDto();
        NormalizedHsRecordDto record = NormalizedHsRecordDto.builder()
                .customsTerritory("IN")
                .country("India")
                .chapter("27")
                .heading("2710")
                .hs6("281019") // HS6 281019 does not start with heading 2710!
                .nationalCode("27101920")
                .officialDescription("Motor Spirit")
                .build();

        List<NormalizedHsRecordDto> valid = validationService.validate(List.of(record), report, 1L);

        assertEquals(0, valid.size());
        assertEquals(1, report.getTotalInvalid());
    }
}
