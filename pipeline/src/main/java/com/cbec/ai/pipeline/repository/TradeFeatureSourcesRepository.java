package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.TradeFeatureSourcesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeFeatureSourcesRepository extends JpaRepository<TradeFeatureSourcesEntity, Long> {
    List<TradeFeatureSourcesEntity> findByFeatureName(String featureName);
    List<TradeFeatureSourcesEntity> findByCountry(String country);
}
