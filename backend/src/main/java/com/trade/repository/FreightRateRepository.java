package com.trade.repository;

import com.trade.entity.FreightRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FreightRateRepository extends JpaRepository<FreightRate, Long> {

    List<FreightRate> findByMode(String mode);

    Optional<FreightRate> findByModeAndOriginLocationAndDestinationLocation(
            String mode, String originLocation, String destinationLocation);
}
