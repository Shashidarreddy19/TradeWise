package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.RegulationHsMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegulationHsMappingRepository extends JpaRepository<RegulationHsMappingEntity, Long> {

    List<RegulationHsMappingEntity> findByRegulationId(Long regulationId);

    List<RegulationHsMappingEntity> findByRegulationIdIn(List<Long> regulationIds);

    List<RegulationHsMappingEntity> findByNationalCode(String nationalCode);

    List<RegulationHsMappingEntity> findByHs6(String hs6);

    List<RegulationHsMappingEntity> findByHeading(String heading);

    List<RegulationHsMappingEntity> findByChapter(String chapter);

    void deleteByRegulationId(Long regulationId);
}
