package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.ComplianceComplexityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplianceComplexityRepository extends JpaRepository<ComplianceComplexityEntity, Long> {

    Optional<ComplianceComplexityEntity> findByCountryAndHsCode(String country, String hsCode);

    List<ComplianceComplexityEntity> findByCountry(String country);

    List<ComplianceComplexityEntity> findByHsCode(String hsCode);
}
