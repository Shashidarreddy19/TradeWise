package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.RegulationHsEvidenceVerificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegulationHsEvidenceVerificationRepository extends JpaRepository<RegulationHsEvidenceVerificationEntity, Long> {
    Optional<RegulationHsEvidenceVerificationEntity> findByCountryAndHsCode(String country, String hsCode);
    List<RegulationHsEvidenceVerificationEntity> findByCountry(String country);
    long countByCountry(String country);
    long countByCountryAndVerificationStatus(String country, String verificationStatus);
    long countByVerificationStatus(String verificationStatus);
    long countByEvidenceLevel(String evidenceLevel);
}
