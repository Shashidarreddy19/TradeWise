package com.trade.service.impl;

import com.trade.dto.dashboard.ExporterDashboardResponse;
import com.trade.dto.dashboard.LogisticsDashboardResponse;
import com.trade.entity.OrderStatus;
import com.trade.entity.ShipmentStatus;
import com.trade.entity.User;
import com.trade.exception.ResourceNotFoundException;
import com.trade.repository.OrderRepository;
import com.trade.repository.ProductRepository;
import com.trade.repository.ShipmentRepository;
import com.trade.repository.UserRepository;
import com.trade.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;

    @Override
    @Transactional(readOnly = true)
    public ExporterDashboardResponse getExporterDashboard(String exporterEmail) {
        User exporter = findUserByEmail(exporterEmail);

        long totalProducts = productRepository.countByExporter(exporter);

        long pendingLogisticsRequests =
                orderRepository.countByExporterAndStatus(exporter, OrderStatus.PENDING_LOGISTICS);

        long acceptedByLogistics =
                orderRepository.countByExporterAndStatus(exporter, OrderStatus.LOGISTICS_ACCEPTED);

        long rejectedRequests =
                orderRepository.countByExporterAndStatus(exporter, OrderStatus.REJECTED);

        long inTransit =
                orderRepository.countByExporterAndStatus(exporter, OrderStatus.IN_TRANSIT);

        long delivered =
                orderRepository.countByExporterAndStatus(exporter, OrderStatus.DELIVERED);

        return ExporterDashboardResponse.builder()
                .totalProducts(totalProducts)
                .pendingLogisticsRequests(pendingLogisticsRequests)
                .acceptedByLogistics(acceptedByLogistics)
                .rejectedRequests(rejectedRequests)
                .inTransit(inTransit)
                .delivered(delivered)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public LogisticsDashboardResponse getLogisticsDashboard(String partnerEmail) {
        User partner = findUserByEmail(partnerEmail);

        // Available requests = all PENDING_LOGISTICS (full count, not filtered per partner here)
        long availableRequests = orderRepository.countByStatus(OrderStatus.PENDING_LOGISTICS);

        // Accepted shipments = all shipments this partner owns (any status)
        long acceptedShipments = shipmentRepository.countByLogisticsPartner(partner);

        // Active = shipments in transit stages
        long activeShipments =
                shipmentRepository.countByLogisticsPartnerAndShipmentStatus(partner, ShipmentStatus.PICKED_UP)
                + shipmentRepository.countByLogisticsPartnerAndShipmentStatus(partner, ShipmentStatus.AT_EXPORT_CUSTOMS)
                + shipmentRepository.countByLogisticsPartnerAndShipmentStatus(partner, ShipmentStatus.IN_TRANSIT)
                + shipmentRepository.countByLogisticsPartnerAndShipmentStatus(partner, ShipmentStatus.AT_IMPORT_CUSTOMS)
                + shipmentRepository.countByLogisticsPartnerAndShipmentStatus(partner, ShipmentStatus.OUT_FOR_DELIVERY);

        long completedDeliveries =
                shipmentRepository.countByLogisticsPartnerAndShipmentStatus(partner, ShipmentStatus.DELIVERED);

        return LogisticsDashboardResponse.builder()
                .availableRequests(availableRequests)
                .acceptedShipments(acceptedShipments)
                .activeShipments(activeShipments)
                .completedDeliveries(completedDeliveries)
                .build();
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}
