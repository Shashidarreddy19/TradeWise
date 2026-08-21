package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.RegulationProcedureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegulationProcedureRepository extends JpaRepository<RegulationProcedureEntity, Long> {

    List<RegulationProcedureEntity> findByRegulationId(Long regulationId);

    List<RegulationProcedureEntity> findByRegulationIdIn(List<Long> regulationIds);

    void deleteByRegulationId(Long regulationId);
}
