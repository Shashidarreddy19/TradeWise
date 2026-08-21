package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.RegulationDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegulationDocumentRepository extends JpaRepository<RegulationDocumentEntity, Long> {

    List<RegulationDocumentEntity> findByRegulationId(Long regulationId);

    List<RegulationDocumentEntity> findByRegulationIdIn(List<Long> regulationIds);

    void deleteByRegulationId(Long regulationId);
}
