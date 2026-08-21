package com.cbec.ai.pipeline.stage;

import com.cbec.ai.pipeline.model.dto.ExtractionReportDto;
import com.cbec.ai.pipeline.model.dto.NormalizedHsRecordDto;
import com.cbec.ai.pipeline.model.entity.CategoryChapterEntity;
import com.cbec.ai.pipeline.model.entity.CategoryMasterEntity;
import com.cbec.ai.pipeline.model.entity.HsValidatedEntity;
import com.cbec.ai.pipeline.model.entity.RejectedRecordEntity;
import com.cbec.ai.pipeline.repository.CategoryChapterRepository;
import com.cbec.ai.pipeline.repository.CategoryMasterRepository;
import com.cbec.ai.pipeline.repository.HsValidatedRepository;
import com.cbec.ai.pipeline.repository.RejectedRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class Stage5CategoryMappingService {

    private final CategoryMasterRepository categoryMasterRepository;
    private final CategoryChapterRepository categoryChapterRepository;
    private final HsValidatedRepository hsValidatedRepository;
    private final RejectedRecordRepository rejectedRecordRepository;

    public Stage5CategoryMappingService(
            CategoryMasterRepository categoryMasterRepository,
            CategoryChapterRepository categoryChapterRepository,
            HsValidatedRepository hsValidatedRepository,
            RejectedRecordRepository rejectedRecordRepository) {
        this.categoryMasterRepository = categoryMasterRepository;
        this.categoryChapterRepository = categoryChapterRepository;
        this.hsValidatedRepository = hsValidatedRepository;
        this.rejectedRecordRepository = rejectedRecordRepository;
    }

    /**
     * Stage 5: Category Mapping & Supported Chapter Filtering.
     * Maps validated records to target categories using normalized relational JOINs (`category_chapter`).
     */
    public List<NormalizedHsRecordDto> mapCategories(List<NormalizedHsRecordDto> records, ExtractionReportDto report, Long executionId) {
        log.info("Stage 5 [Category Mapping] - Mapping categories using normalized category_chapter relational table for {} records", records.size());

        List<NormalizedHsRecordDto> mappedRecords = new ArrayList<>();
        List<HsValidatedEntity> validatedEntitiesToSave = new ArrayList<>();
        List<RejectedRecordEntity> rejectionsToSave = new ArrayList<>();

        for (NormalizedHsRecordDto record : records) {
            String chapter = record.getChapter();
            String matchedCategory = resolveCategoryFromRelationalTable(chapter);

            if (matchedCategory == null) {
                String reason = "UNSUPPORTED_CHAPTER: Chapter '" + chapter + "' not in target category_chapter master";
                report.setTotalSkipped(report.getTotalSkipped() + 1);
                report.getRejectionSummary().add(reason + " (Code: " + record.getNationalCode() + ")");

                rejectionsToSave.add(RejectedRecordEntity.builder()
                        .executionId(executionId)
                        .rawRecord("National Code: " + record.getNationalCode() + " | Chapter: " + chapter)
                        .reason(reason)
                        .pipelineStage("Stage 5 Category Mapping")
                        .country(record.getCountry() != null ? record.getCountry() : "UNKNOWN")
                        .source(record.getSource() != null ? record.getSource() : "OFFICIAL_SOURCE")
                        .build());
                continue;
            }

            record.setCategory(matchedCategory);
            mappedRecords.add(record);

            if (executionId != null) {
                validatedEntitiesToSave.add(HsValidatedEntity.builder()
                        .executionId(executionId)
                        .customsTerritory(record.getCustomsTerritory() != null ? record.getCustomsTerritory() : "GLOBAL")
                        .country(record.getCountry())
                        .chapter(record.getChapter())
                        .heading(record.getHeading())
                        .hs6(record.getHs6())
                        .nationalCode(record.getNationalCode())
                        .codeLength(record.getCodeLength())
                        .nomenclatureType(record.getNomenclatureType() != null ? record.getNomenclatureType() : "NATIONAL_TARIFF")
                        .category(record.getCategory())
                        .officialDescription(record.getOfficialDescription())
                        .unit(record.getUnit())
                        .sourceName(record.getSource())
                        .datasetVersion(record.getVersion())
                        .build());
            }
        }

        if (!validatedEntitiesToSave.isEmpty()) {
            hsValidatedRepository.saveAll(validatedEntitiesToSave);
            log.info("Saved {} clean validated records into hs_validated table for execution ID {}", validatedEntitiesToSave.size(), executionId);
        }

        if (!rejectionsToSave.isEmpty()) {
            rejectedRecordRepository.saveAll(rejectionsToSave);
            log.info("Saved {} unsupported chapter rejections into rejected_records table", rejectionsToSave.size());
        }

        log.info("Stage 5 [Category Mapping] - Relational category mapping finished. Target category records: {}, Skipped: {}",
                mappedRecords.size(), report.getTotalSkipped());
        return mappedRecords;
    }

    private String resolveCategoryFromRelationalTable(String chapter) {
        if (chapter == null || chapter.trim().isEmpty()) return null;
        String normalizedCh = chapter.trim();
        if (normalizedCh.length() == 1) normalizedCh = "0" + normalizedCh;

        List<CategoryChapterEntity> matches = categoryChapterRepository.findByChapter(normalizedCh);
        if (!matches.isEmpty()) {
            return matches.get(0).getCategory().getCategoryName();
        }

        // Try single-digit string match
        if (normalizedCh.startsWith("0")) {
            matches = categoryChapterRepository.findByChapter(normalizedCh.substring(1));
            if (!matches.isEmpty()) {
                return matches.get(0).getCategory().getCategoryName();
            }
        }
        return null;
    }
}
