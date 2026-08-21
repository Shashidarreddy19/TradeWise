package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.HsRawEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HsRawRepository extends JpaRepository<HsRawEntity, Long> {
    List<HsRawEntity> findByExecutionId(Long executionId);
    List<HsRawEntity> findByCountry(String country);
}
