package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.RegulationEvidenceAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegulationEvidenceAuditRepository extends JpaRepository<RegulationEvidenceAuditEntity, Long> {
    List<RegulationEvidenceAuditEntity> findByCountry(String country);
    List<RegulationEvidenceAuditEntity> findByRegulationId(Long regulationId);
}
