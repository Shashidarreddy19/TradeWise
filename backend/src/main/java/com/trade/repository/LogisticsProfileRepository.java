package com.trade.repository;

import com.trade.entity.LogisticsProfile;
import com.trade.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LogisticsProfileRepository extends JpaRepository<LogisticsProfile, Long> {

    Optional<LogisticsProfile> findByUser(User user);

    Optional<LogisticsProfile> findByUserId(Long userId);
}
