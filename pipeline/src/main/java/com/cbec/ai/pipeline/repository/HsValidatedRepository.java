package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.HsValidatedEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HsValidatedRepository extends JpaRepository<HsValidatedEntity, Long> {
    List<HsValidatedEntity> findByExecutionId(Long executionId);
    List<HsValidatedEntity> findByCountry(String country);
}
