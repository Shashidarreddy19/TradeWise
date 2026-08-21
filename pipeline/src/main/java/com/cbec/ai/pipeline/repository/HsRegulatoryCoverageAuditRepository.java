package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.HsRegulatoryCoverageAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HsRegulatoryCoverageAuditRepository extends JpaRepository<HsRegulatoryCoverageAuditEntity, Long> {

    List<HsRegulatoryCoverageAuditEntity> findByCountry(String country);

    Optional<HsRegulatoryCoverageAuditEntity> findByCountryAndHsCode(String country, String hsCode);

    List<HsRegulatoryCoverageAuditEntity> findByCountryAndCoverageStatus(String country, String coverageStatus);

    List<HsRegulatoryCoverageAuditEntity> findByCountryAndHs6(String country, String hs6);

    long countByCountryAndRegulationFound(String country, boolean regulationFound);

    long countByCountryAndCoverageStatus(String country, String coverageStatus);

    long countByCountry(String country);

    @Query("SELECT DISTINCT h.country FROM HsRegulatoryCoverageAuditEntity h")
    List<String> findDistinctCountries();

    @Query("SELECT COUNT(h) FROM HsRegulatoryCoverageAuditEntity h WHERE h.country = :country AND h.evidenceComplete = true")
    long countCompleteByCountry(String country);
}
