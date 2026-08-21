package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.RegulationRestrictionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegulationRestrictionRepository extends JpaRepository<RegulationRestrictionEntity, Long> {

    List<RegulationRestrictionEntity> findByRegulationId(Long regulationId);

    List<RegulationRestrictionEntity> findByRegulationIdIn(List<Long> regulationIds);

    void deleteByRegulationId(Long regulationId);
}
