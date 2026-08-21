package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.TradeDataQualityAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeDataQualityAuditRepository extends JpaRepository<TradeDataQualityAuditEntity, Long> {
    List<TradeDataQualityAuditEntity> findByCountry(String country);
    List<TradeDataQualityAuditEntity> findByCheckType(String checkType);
}
