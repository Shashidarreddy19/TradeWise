package com.trade.repository;

import com.trade.entity.Port;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortRepository extends JpaRepository<Port, Long> {

    Optional<Port> findByUnlocode(String unlocode);

    List<Port> findByCountryCode(String countryCode);

    List<Port> findByPortType(String portType);

    List<Port> findByCountryCodeAndPortType(String countryCode, String portType);
}
