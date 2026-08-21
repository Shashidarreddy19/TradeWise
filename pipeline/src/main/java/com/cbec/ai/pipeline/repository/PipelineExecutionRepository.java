package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.PipelineExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PipelineExecutionRepository extends JpaRepository<PipelineExecutionEntity, Long> {
    List<PipelineExecutionEntity> findByCountry(String country);
    List<PipelineExecutionEntity> findByStatus(String status);
}
