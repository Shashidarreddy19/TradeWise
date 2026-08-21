package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.TradeImportStatisticsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradeImportStatisticsRepository extends JpaRepository<TradeImportStatisticsEntity, Long> {
    Optional<TradeImportStatisticsEntity> findByHs6AndDestinationCountryAndYear(String hs6, String destinationCountry, Integer year);
    List<TradeImportStatisticsEntity> findByDestinationCountry(String destinationCountry);
    List<TradeImportStatisticsEntity> findByHs6(String hs6);
    List<TradeImportStatisticsEntity> findByHs6AndDestinationCountry(String hs6, String destinationCountry);
}
