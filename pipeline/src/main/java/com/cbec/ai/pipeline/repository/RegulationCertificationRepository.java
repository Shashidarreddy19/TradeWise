package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.RegulationCertificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegulationCertificationRepository extends JpaRepository<RegulationCertificationEntity, Long> {

    List<RegulationCertificationEntity> findByRegulationId(Long regulationId);

    List<RegulationCertificationEntity> findByRegulationIdIn(List<Long> regulationIds);

    void deleteByRegulationId(Long regulationId);
}
