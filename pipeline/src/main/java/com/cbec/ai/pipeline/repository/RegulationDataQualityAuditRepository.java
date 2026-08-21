package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.RegulationDataQualityAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegulationDataQualityAuditRepository extends JpaRepository<RegulationDataQualityAuditEntity, Long> {
    List<RegulationDataQualityAuditEntity> findByCountry(String country);
}
