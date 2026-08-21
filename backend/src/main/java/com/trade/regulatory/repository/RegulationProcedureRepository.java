package com.trade.regulatory.repository;

import com.trade.regulatory.entity.RegulationProcedureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegulationProcedureRepository extends JpaRepository<RegulationProcedureEntity, Long> {
    List<RegulationProcedureEntity> findByRegulationIdOrderByStepOrderAsc(Long regulationId);
    List<RegulationProcedureEntity> findByRegulationIdIn(List<Long> regulationIds);
}
