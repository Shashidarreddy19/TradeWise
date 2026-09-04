package com.trade.repository;

import com.trade.entity.Incoterm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IncotermRepository extends JpaRepository<Incoterm, Long> {

    Optional<Incoterm> findByCode(String code);
}
