package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.RegulationHsMappingAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegulationHsMappingAuditRepository extends JpaRepository<RegulationHsMappingAuditEntity, Long> {
    List<RegulationHsMappingAuditEntity> findByCountry(String country);
}
