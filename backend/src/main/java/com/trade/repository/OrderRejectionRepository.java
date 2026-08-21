package com.trade.repository;

import com.trade.entity.Order;
import com.trade.entity.OrderRejection;
import com.trade.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRejectionRepository extends JpaRepository<OrderRejection, Long> {

    boolean existsByOrderAndLogisticsPartner(Order order, User logisticsPartner);

    long countByOrder(Order order);
}
