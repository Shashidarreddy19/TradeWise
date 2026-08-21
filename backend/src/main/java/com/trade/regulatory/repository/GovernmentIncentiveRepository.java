package com.trade.regulatory.repository;

import com.trade.regulatory.entity.GovernmentIncentiveEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GovernmentIncentiveRepository
        extends JpaRepository<GovernmentIncentiveEntity, Long> {

    List<GovernmentIncentiveEntity> findByCountryAndStatus(String country, String status);

    List<GovernmentIncentiveEntity> findByCountry(String country);

    @Query("SELECT g FROM GovernmentIncentiveEntity g WHERE g.country = :country " +
           "AND g.status = 'ACTIVE' AND (" +
           "g.chapterApplicable = 'ALL' OR " +
           "g.hsCodeApplicable = 'ALL' OR " +
           "g.chapterApplicable LIKE CONCAT(:chapter, '%') OR " +
           "g.hsCodeApplicable LIKE CONCAT(:hs6, '%'))")
    List<GovernmentIncentiveEntity> findApplicable(
            @Param("country") String country,
            @Param("chapter") String chapter,
            @Param("hs6") String hs6);
}
