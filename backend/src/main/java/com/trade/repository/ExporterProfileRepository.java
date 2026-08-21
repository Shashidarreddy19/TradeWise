package com.trade.repository;

import com.trade.entity.ExporterProfile;
import com.trade.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExporterProfileRepository extends JpaRepository<ExporterProfile, Long> {

    Optional<ExporterProfile> findByUser(User user);

    Optional<ExporterProfile> findByUserId(Long userId);

    boolean existsByGstNumber(String gstNumber);
}
