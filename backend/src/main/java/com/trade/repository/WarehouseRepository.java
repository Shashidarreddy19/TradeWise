package com.trade.repository;

import com.trade.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    List<Warehouse> findByCountryCode(String countryCode);

    List<Warehouse> findByWarehouseType(String warehouseType);
}
