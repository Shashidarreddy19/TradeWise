package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.CbecCountryRecommendationDatasetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CbecCountryRecommendationDatasetRepository extends JpaRepository<CbecCountryRecommendationDatasetEntity, Long> {
    Optional<CbecCountryRecommendationDatasetEntity> findByHs6AndDestinationCountryAndYear(String hs6, String destinationCountry, Integer year);
    List<CbecCountryRecommendationDatasetEntity> findByDestinationCountry(String destinationCountry);
    List<CbecCountryRecommendationDatasetEntity> findByHs6(String hs6);
}
