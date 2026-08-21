package com.trade.regulatory.repository;

import com.trade.regulatory.entity.RegulationRestrictionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegulationRestrictionRepository extends JpaRepository<RegulationRestrictionEntity, Long> {
    List<RegulationRestrictionEntity> findByRegulationId(Long regulationId);
    List<RegulationRestrictionEntity> findByRegulationIdIn(List<Long> regulationIds);
}
