package com.trade.regulatory.repository;

import com.trade.regulatory.entity.RegulationHsMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegulationHsMappingRepository extends JpaRepository<RegulationHsMappingEntity, Long> {

    // Exact national code match (highest priority)
    List<RegulationHsMappingEntity> findByNationalCode(String nationalCode);

    // HS6 match
    List<RegulationHsMappingEntity> findByHs6(String hs6);

    // Heading (HS4) match
    List<RegulationHsMappingEntity> findByHeading(String heading);

    // Chapter (HS2) match
    List<RegulationHsMappingEntity> findByChapter(String chapter);

    /**
     * Hierarchical lookup: try exact → HS6 → HS4 → HS2 in one query.
     * Returns all matches; caller picks the strongest.
     */
    @Query("SELECT m FROM RegulationHsMappingEntity m WHERE " +
           "m.nationalCode = :nationalCode OR " +
           "m.hs6 = :hs6 OR " +
           "m.heading = :heading OR " +
           "m.chapter = :chapter " +
           "ORDER BY CASE " +
           "  WHEN m.nationalCode = :nationalCode THEN 1 " +
           "  WHEN m.hs6 = :hs6 THEN 2 " +
           "  WHEN m.heading = :heading THEN 3 " +
           "  WHEN m.chapter = :chapter THEN 4 " +
           "  ELSE 5 END")
    List<RegulationHsMappingEntity> findHierarchical(
            @Param("nationalCode") String nationalCode,
            @Param("hs6") String hs6,
            @Param("heading") String heading,
            @Param("chapter") String chapter);

    /**
     * Country-aware hierarchical lookup: only returns mappings for regulations belonging to the specified country.
     */
    @Query("SELECT m FROM RegulationHsMappingEntity m, RegulationMasterEntity r WHERE " +
           "m.regulationId = r.id AND " +
           "(LOWER(r.country) = LOWER(:country) OR LOWER(r.country) = LOWER(:countryCode)) AND " +
           "(m.nationalCode = :nationalCode OR " +
           " m.hs6 = :hs6 OR " +
           " m.heading = :heading OR " +
           " m.chapter = :chapter) " +
           "ORDER BY CASE " +
           "  WHEN m.nationalCode = :nationalCode THEN 1 " +
           "  WHEN m.hs6 = :hs6 THEN 2 " +
           "  WHEN m.heading = :heading THEN 3 " +
           "  WHEN m.chapter = :chapter THEN 4 " +
           "  ELSE 5 END")
    List<RegulationHsMappingEntity> findHierarchicalByCountry(
            @Param("country") String country,
            @Param("countryCode") String countryCode,
            @Param("nationalCode") String nationalCode,
            @Param("hs6") String hs6,
            @Param("heading") String heading,
            @Param("chapter") String chapter);
}
