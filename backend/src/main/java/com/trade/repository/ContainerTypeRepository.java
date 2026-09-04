package com.trade.repository;

import com.trade.entity.ContainerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContainerTypeRepository extends JpaRepository<ContainerType, Long> {

    Optional<ContainerType> findByCode(String code);
}
