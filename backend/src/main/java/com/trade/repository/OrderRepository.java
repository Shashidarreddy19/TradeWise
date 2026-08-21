package com.trade.repository;

import com.trade.entity.Order;
import com.trade.entity.OrderStatus;
import com.trade.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Exporter's own requests
    List<Order> findByExporter(User exporter);

    List<Order> findByExporterId(Long exporterId);

    // All pending requests — visible to every logistics partner
    List<Order> findByStatus(OrderStatus status);

    // Count by exporter + status — for exporter dashboard
    long countByExporterAndStatus(User exporter, OrderStatus status);

    long countByExporter(User exporter);

    // Count all orders by status — for logistics dashboard
    long countByStatus(OrderStatus status);

    // Count logistics partners in the system — used to determine if all rejected
    long countByAssignedLogisticsPartner(User logisticsPartner);
}
