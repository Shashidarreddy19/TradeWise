package com.trade.regulatory.repository;

import com.trade.regulatory.entity.RegulationLabelingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegulationLabelingRepository extends JpaRepository<RegulationLabelingEntity, Long> {
    List<RegulationLabelingEntity> findByRegulationId(Long regulationId);
    List<RegulationLabelingEntity> findByRegulationIdIn(List<Long> regulationIds);
}
