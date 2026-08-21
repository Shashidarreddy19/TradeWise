package com.trade.regulatory.repository;

import com.trade.regulatory.entity.HsRegulatoryCoverageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HsRegulatoryCoverageRepository extends JpaRepository<HsRegulatoryCoverageEntity, Long> {

    Optional<HsRegulatoryCoverageEntity> findByCountryAndHsCode(String country, String hsCode);

    List<HsRegulatoryCoverageEntity> findByCountryAndHs6(String country, String hs6);

    @Query("SELECT h.coverageStatus, COUNT(h) FROM HsRegulatoryCoverageEntity h " +
           "WHERE h.country = :country GROUP BY h.coverageStatus")
    List<Object[]> countByCoverageStatus(@Param("country") String country);

    long countByCountry(String country);
}
