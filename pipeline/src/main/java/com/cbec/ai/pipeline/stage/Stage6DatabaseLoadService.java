package com.cbec.ai.pipeline.stage;

import com.cbec.ai.pipeline.model.dto.ExtractionReportDto;
import com.cbec.ai.pipeline.model.dto.NormalizedHsRecordDto;
import com.cbec.ai.pipeline.model.entity.HsMasterEntity;
import com.cbec.ai.pipeline.model.entity.HsVersionEntity;
import com.cbec.ai.pipeline.repository.HsMasterRepository;
import com.cbec.ai.pipeline.repository.HsVersionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class Stage6DatabaseLoadService {

    private final HsMasterRepository hsMasterRepository;
    private final HsVersionRepository hsVersionRepository;

    public Stage6DatabaseLoadService(HsMasterRepository hsMasterRepository, HsVersionRepository hsVersionRepository) {
        this.hsMasterRepository = hsMasterRepository;
        this.hsVersionRepository = hsVersionRepository;
    }

    /**
     * Stage 6: Master Database Loading into `hs_master` and `hs_versions`.
     * Calculates SHA-256 record content hash, maintains `is_current` flags, and logs version revisions without overwriting history.
     */
    @Transactional
    public void loadIntoDatabase(List<NormalizedHsRecordDto> records, ExtractionReportDto report) {
        log.info("Stage 6 [Database Load] - Batch loading {} records into 'hs_master' and 'hs_versions'", records.size());

        List<HsMasterEntity> masterEntitiesToSave = new ArrayList<>();
        List<HsVersionEntity> versionEntitiesToSave = new ArrayList<>();

        for (NormalizedHsRecordDto dto : records) {
            String territory = (dto.getCustomsTerritory() != null && !dto.getCustomsTerritory().isEmpty()) ? dto.getCustomsTerritory() : "GLOBAL";
            String version = (dto.getVersion() != null && !dto.getVersion().isEmpty()) ? dto.getVersion() : "2026.1";
            String recordHash = computeRecordHash(dto.getNationalCode(), dto.getOfficialDescription(), dto.getUnit());

            Optional<HsMasterEntity> existingOpt = hsMasterRepository.findByCustomsTerritoryAndNationalCodeAndDatasetVersion(
                    territory, dto.getNationalCode(), version);

            HsMasterEntity masterEntity;
            LocalDateTime now = LocalDateTime.now();

            if (existingOpt.isPresent()) {
                masterEntity = existingOpt.get();

                // If content hash changed, update record and log historical version
                if (masterEntity.getRecordHash() == null || !masterEntity.getRecordHash().equals(recordHash)) {
                    List<HsVersionEntity> existingVersions = hsVersionRepository.findByHsMasterId(masterEntity.getId());
                    for (HsVersionEntity v : existingVersions) {
                        if (v.getEffectiveTo() == null) {
                            v.setEffectiveTo(now);
                            hsVersionRepository.save(v);
                        }
                    }

                    versionEntitiesToSave.add(HsVersionEntity.builder()
                            .hsMasterId(masterEntity.getId())
                            .customsTerritory(territory)
                            .country(dto.getCountry())
                            .nationalCode(dto.getNationalCode())
                            .officialDescription(dto.getOfficialDescription())
                            .category(dto.getCategory())
                            .version(version)
                            .effectiveFrom(now)
                            .lastVerified(dto.getLastVerified())
                            .build());
                }

                masterEntity.setOfficialDescription(dto.getOfficialDescription());
                masterEntity.setUnit(dto.getUnit());
                masterEntity.setCategory(dto.getCategory());
                masterEntity.setRecordHash(recordHash);
                masterEntity.setIsCurrent(true);
                masterEntity.setIsActive(true);
                masterEntity.setLastVerified(dto.getLastVerified());

                report.setTotalDuplicates(report.getTotalDuplicates() + 1);
            } else {
                masterEntity = HsMasterEntity.builder()
                        .customsTerritory(territory)
                        .country(dto.getCountry())
                        .chapter(dto.getChapter())
                        .heading(dto.getHeading())
                        .hs6(dto.getHs6())
                        .nationalCode(dto.getNationalCode())
                        .codeLength(dto.getCodeLength())
                        .nomenclatureType(dto.getNomenclatureType() != null ? dto.getNomenclatureType() : "NATIONAL_TARIFF")
                        .category(dto.getCategory())
                        .officialDescription(dto.getOfficialDescription())
                        .unit(dto.getUnit())
                        .datasetVersion(version)
                        .isCurrent(true)
                        .isActive(true)
                        .recordHash(recordHash)
                        .effectiveFrom(now)
                        .lastVerified(dto.getLastVerified())
                        .build();

                report.setTotalInserted(report.getTotalInserted() + 1);
            }

            masterEntitiesToSave.add(masterEntity);
        }

        // Batch save master entities
        List<HsMasterEntity> savedMasters = hsMasterRepository.saveAll(masterEntitiesToSave);

        // Record initial version audits for new masters
        for (HsMasterEntity master : savedMasters) {
            List<HsVersionEntity> existingVersions = hsVersionRepository.findByHsMasterId(master.getId());
            if (existingVersions.isEmpty()) {
                versionEntitiesToSave.add(HsVersionEntity.builder()
                        .hsMasterId(master.getId())
                        .customsTerritory(master.getCustomsTerritory())
                        .country(master.getCountry())
                        .nationalCode(master.getNationalCode())
                        .officialDescription(master.getOfficialDescription())
                        .category(master.getCategory())
                        .version(master.getDatasetVersion())
                        .effectiveFrom(master.getCreatedAt() != null ? master.getCreatedAt() : LocalDateTime.now())
                        .lastVerified(master.getLastVerified())
                        .build());
            }
        }

        if (!versionEntitiesToSave.isEmpty()) {
            hsVersionRepository.saveAll(versionEntitiesToSave);
            log.info("Saved {} historical version audit records into hs_versions table", versionEntitiesToSave.size());
        }

        log.info("Stage 6 [Database Load] - Master batch load completed successfully for {} records with record hashing", savedMasters.size());
    }

    private String computeRecordHash(String code, String desc, String unit) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String payload = (code != null ? code : "") + "|" + (desc != null ? desc : "") + "|" + (unit != null ? unit : "");
            byte[] hashBytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            return "";
        }
    }
}
