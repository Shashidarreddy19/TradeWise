package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.HsMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HsMasterRepository extends JpaRepository<HsMasterEntity, Long> {

    Optional<HsMasterEntity> findByCustomsTerritoryAndNationalCodeAndDatasetVersion(String customsTerritory, String nationalCode, String datasetVersion);

    Optional<HsMasterEntity> findByCustomsTerritoryAndNationalCode(String customsTerritory, String nationalCode);

    Optional<HsMasterEntity> findByCountryAndNationalCode(String country, String nationalCode);

    List<HsMasterEntity> findByCountryAndHs6(String country, String hs6);

    List<HsMasterEntity> findByCountryAndHeading(String country, String heading);

    List<HsMasterEntity> findByCountryAndChapter(String country, String chapter);

    List<HsMasterEntity> findByCountry(String country);

    List<HsMasterEntity> findByCustomsTerritory(String customsTerritory);

    List<HsMasterEntity> findByCategory(String category);

    @Query("SELECT COUNT(h) FROM HsMasterEntity h WHERE h.country = :country")
    long countByCountry(String country);
}
