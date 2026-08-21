package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.RejectedRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RejectedRecordRepository extends JpaRepository<RejectedRecordEntity, Long> {
    List<RejectedRecordEntity> findByExecutionId(Long executionId);
    List<RejectedRecordEntity> findByCountry(String country);
}
