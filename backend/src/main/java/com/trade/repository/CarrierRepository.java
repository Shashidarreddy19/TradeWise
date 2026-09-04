package com.trade.repository;

import com.trade.entity.Carrier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarrierRepository extends JpaRepository<Carrier, Long> {

    List<Carrier> findByCarrierType(String carrierType);
}
