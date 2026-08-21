package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.CountryMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CountryMasterRepository extends JpaRepository<CountryMasterEntity, Long> {
    Optional<CountryMasterEntity> findByCountryName(String countryName);
    Optional<CountryMasterEntity> findByCountryCode(String countryCode);
}
