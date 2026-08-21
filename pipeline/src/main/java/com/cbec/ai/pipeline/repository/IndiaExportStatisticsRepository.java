package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.IndiaExportStatisticsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IndiaExportStatisticsRepository extends JpaRepository<IndiaExportStatisticsEntity, Long> {
    Optional<IndiaExportStatisticsEntity> findByHs6AndDestinationCountryAndYear(String hs6, String destinationCountry, Integer year);
    List<IndiaExportStatisticsEntity> findByDestinationCountry(String destinationCountry);
    List<IndiaExportStatisticsEntity> findByHs6(String hs6);
    List<IndiaExportStatisticsEntity> findByHs6AndDestinationCountry(String hs6, String destinationCountry);
}
