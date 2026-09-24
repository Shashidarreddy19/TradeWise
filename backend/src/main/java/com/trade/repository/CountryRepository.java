package com.trade.repository;

import com.trade.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CountryRepository extends JpaRepository<Country, Long> {

    Optional<Country> findByName(String name);

    Optional<Country> findByNameIgnoreCase(String name);

    Optional<Country> findByCode(String code);

    Optional<Country> findByCodeIgnoreCase(String code);

    boolean existsByName(String name);

    boolean existsByCode(String code);
}

