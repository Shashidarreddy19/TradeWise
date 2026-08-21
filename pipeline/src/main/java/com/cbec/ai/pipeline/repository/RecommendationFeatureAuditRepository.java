package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.RecommendationFeatureAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationFeatureAuditRepository extends JpaRepository<RecommendationFeatureAuditEntity, Long> {
    List<RecommendationFeatureAuditEntity> findByCountry(String country);
    List<RecommendationFeatureAuditEntity> findByAuditStatus(String auditStatus);
}
