package com.trade.regulatory.repository;

import com.trade.regulatory.entity.CountryMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CountryMasterRepository extends JpaRepository<CountryMasterEntity, Long> {
    Optional<CountryMasterEntity> findByCountryCode(String countryCode);
    Optional<CountryMasterEntity> findByCountryName(String countryName);
    List<CountryMasterEntity> findByActiveTrue();
}
