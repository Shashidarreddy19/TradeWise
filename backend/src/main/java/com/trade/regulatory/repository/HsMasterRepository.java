package com.trade.regulatory.repository;

import com.trade.regulatory.entity.HsMasterEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HsMasterRepository extends JpaRepository<HsMasterEntity, Long> {

    // Exact national code lookup
    Optional<HsMasterEntity> findByCountryAndNationalCodeAndIsCurrentTrue(String country, String nationalCode);

    // HS6 level lookup
    List<HsMasterEntity> findByCountryAndHs6AndIsCurrentTrue(String country, String hs6);

    // HS4 heading level
    List<HsMasterEntity> findByCountryAndHeadingAndIsCurrentTrue(String country, String heading);

    // HS2 chapter level
    List<HsMasterEntity> findByCountryAndChapterAndIsCurrentTrue(String country, String chapter);

    // Search by description (keyword search)
    @Query("SELECT h FROM HsMasterEntity h WHERE h.isCurrent = true AND " +
           "LOWER(h.officialDescription) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY h.country, h.nationalCode")
    Page<HsMasterEntity> searchByDescription(@Param("query") String query, Pageable pageable);

    // Search by code prefix
    @Query("SELECT h FROM HsMasterEntity h WHERE h.isCurrent = true AND " +
           "h.nationalCode LIKE CONCAT(:prefix, '%') " +
           "ORDER BY h.country, h.nationalCode")
    Page<HsMasterEntity> searchByCodePrefix(@Param("prefix") String prefix, Pageable pageable);

    // Country + code search
    @Query("SELECT h FROM HsMasterEntity h WHERE h.isCurrent = true AND " +
           "h.country = :country AND " +
           "(h.nationalCode LIKE CONCAT(:query, '%') OR " +
           "LOWER(h.officialDescription) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY h.nationalCode")
    Page<HsMasterEntity> searchByCountryAndQuery(
            @Param("country") String country,
            @Param("query") String query,
            Pageable pageable);

    // Count by territory
    @Query("SELECT h.customsTerritory, COUNT(h) FROM HsMasterEntity h WHERE h.isCurrent = true GROUP BY h.customsTerritory")
    List<Object[]> countByTerritory();

    long countByIsCurrentTrue();

    // ═══════════════ HS Classification queries ═══════════════

    /** Description search on official_description for HS codes */
    @Query("SELECT h FROM HsMasterEntity h WHERE h.country = :country AND h.isCurrent = true " +
           "AND LOWER(h.officialDescription) LIKE LOWER(CONCAT('%', :searchTerms, '%'))")
    List<HsMasterEntity> searchByCountryAndDescriptionLike(@Param("country") String country,
                                                          @Param("searchTerms") String searchTerms);

    /** LIKE search on description for a specific country */
    List<HsMasterEntity> findByCountryAndOfficialDescriptionContainingIgnoreCaseAndIsCurrentTrue(
            String country, String keyword);
}
