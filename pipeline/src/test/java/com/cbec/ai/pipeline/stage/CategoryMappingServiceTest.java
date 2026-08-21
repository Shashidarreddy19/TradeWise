package com.cbec.ai.pipeline.stage;

import com.cbec.ai.pipeline.model.dto.ExtractionReportDto;
import com.cbec.ai.pipeline.model.dto.NormalizedHsRecordDto;
import com.cbec.ai.pipeline.model.entity.CategoryChapterEntity;
import com.cbec.ai.pipeline.model.entity.CategoryMasterEntity;
import com.cbec.ai.pipeline.repository.CategoryChapterRepository;
import com.cbec.ai.pipeline.repository.CategoryMasterRepository;
import com.cbec.ai.pipeline.repository.HsValidatedRepository;
import com.cbec.ai.pipeline.repository.RejectedRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CategoryMappingServiceTest {

    private Stage5CategoryMappingService categoryMappingService;
    private CategoryMasterRepository categoryMasterRepository;
    private CategoryChapterRepository categoryChapterRepository;
    private HsValidatedRepository hsValidatedRepository;
    private RejectedRecordRepository rejectedRecordRepository;

    @BeforeEach
    void setUp() {
        categoryMasterRepository = mock(CategoryMasterRepository.class);
        categoryChapterRepository = mock(CategoryChapterRepository.class);
        hsValidatedRepository = mock(HsValidatedRepository.class);
        rejectedRecordRepository = mock(RejectedRecordRepository.class);

        when(hsValidatedRepository.saveAll(anyList())).thenReturn(List.of());
        when(rejectedRecordRepository.saveAll(anyList())).thenReturn(List.of());

        CategoryMasterEntity cat27 = CategoryMasterEntity.builder().categoryName("Mineral Fuels & Petroleum").build();
        CategoryMasterEntity cat71 = CategoryMasterEntity.builder().categoryName("Gems & Jewellery").build();
        CategoryMasterEntity cat30 = CategoryMasterEntity.builder().categoryName("Pharmaceuticals").build();

        when(categoryChapterRepository.findByChapter("27")).thenReturn(List.of(CategoryChapterEntity.builder().category(cat27).chapter("27").build()));
        when(categoryChapterRepository.findByChapter("71")).thenReturn(List.of(CategoryChapterEntity.builder().category(cat71).chapter("71").build()));
        when(categoryChapterRepository.findByChapter("30")).thenReturn(List.of(CategoryChapterEntity.builder().category(cat30).chapter("30").build()));
        when(categoryChapterRepository.findByChapter("01")).thenReturn(List.of());

        categoryMappingService = new Stage5CategoryMappingService(
                categoryMasterRepository, categoryChapterRepository, hsValidatedRepository, rejectedRecordRepository);
    }

    @Test
    void testCategoryMappingSupportedChaptersFromRelationalTable() {
        ExtractionReportDto report = new ExtractionReportDto();

        NormalizedHsRecordDto rec27 = NormalizedHsRecordDto.builder().chapter("27").nationalCode("27101920").officialDescription("Mineral Fuel").build();
        NormalizedHsRecordDto rec71 = NormalizedHsRecordDto.builder().chapter("71").nationalCode("71023910").officialDescription("Diamond").build();
        NormalizedHsRecordDto rec30 = NormalizedHsRecordDto.builder().chapter("30").nationalCode("30049099").officialDescription("Medicament").build();

        List<NormalizedHsRecordDto> mapped = categoryMappingService.mapCategories(List.of(rec27, rec71, rec30), report, 1L);

        assertEquals(3, mapped.size());
        assertEquals("Mineral Fuels & Petroleum", mapped.get(0).getCategory());
        assertEquals("Gems & Jewellery", mapped.get(1).getCategory());
        assertEquals("Pharmaceuticals", mapped.get(2).getCategory());
    }

    @Test
    void testCategoryMappingSkipsUnsupportedChapters() {
        ExtractionReportDto report = new ExtractionReportDto();

        NormalizedHsRecordDto rec01 = NormalizedHsRecordDto.builder().chapter("01").nationalCode("01012100").officialDescription("Horse").build();

        List<NormalizedHsRecordDto> mapped = categoryMappingService.mapCategories(List.of(rec01), report, 1L);

        assertEquals(0, mapped.size());
        assertEquals(1, report.getTotalSkipped());
    }
}
