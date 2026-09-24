package com.trade.service.impl;

import com.trade.dto.order.OrderRequest;
import com.trade.dto.order.OrderResponse;
import com.trade.entity.*;
import com.trade.exception.BadRequestException;
import com.trade.exception.ResourceNotFoundException;
import com.trade.exception.UnauthorizedException;
import com.trade.repository.*;
import com.trade.service.OrderService;
import com.trade.util.MappingUtil;
import com.trade.util.TrackingNumberUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CountryRepository countryRepository;
    private final UserRepository userRepository;
    private final ShipmentRepository shipmentRepository;
    private final OrderRejectionRepository rejectionRepository;

    // ── Exporter operations ──────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrdersForExporter(String exporterEmail) {
        User exporter = findUserByEmail(exporterEmail);
        return orderRepository.findByExporter(exporter)
                .stream()
                .map(MappingUtil::toOrderResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id, String exporterEmail) {
        Order order = findOrderById(id);
        verifyExporterOwnership(order, exporterEmail);
        return MappingUtil.toOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request, String exporterEmail) {
        User exporter = findUserByEmail(exporterEmail);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        // Exporter can only ship their own products
        if (!product.getExporter().getEmail().equals(exporterEmail)) {
            throw new UnauthorizedException("You can only create shipment requests for your own products");
        }

        Country destination = resolveDestinationCountry(request);

        BigDecimal totalPrice = product.getPrice()
                .multiply(BigDecimal.valueOf(request.getQuantity()));

        Order order = Order.builder()
                .product(product)
                .exporter(exporter)
                .destinationCountry(destination)
                .quantity(request.getQuantity())
                .totalPrice(totalPrice)
                .pickupLocation(request.getPickupLocation())
                .shippingRequirements(request.getShippingRequirements())
                .specialInstructions(request.getSpecialInstructions())
                .status(OrderStatus.PENDING_LOGISTICS)
                .build();

        order = orderRepository.save(order);
        log.info("Shipment request created [id={}] by exporter [{}]", order.getId(), exporterEmail);
        return MappingUtil.toOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateOrder(Long id, OrderRequest request, String exporterEmail) {
        Order order = findOrderById(id);
        verifyExporterOwnership(order, exporterEmail);

        // Only allow edits while still waiting for a logistics partner
        if (order.getStatus() != OrderStatus.PENDING_LOGISTICS) {
            throw new BadRequestException(
                "Only PENDING_LOGISTICS requests can be updated. Current status: " + order.getStatus()
            );
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        Country destination = resolveDestinationCountry(request);

        BigDecimal totalPrice = product.getPrice()
                .multiply(BigDecimal.valueOf(request.getQuantity()));


        order.setProduct(product);
        order.setDestinationCountry(destination);
        order.setQuantity(request.getQuantity());
        order.setTotalPrice(totalPrice);
        order.setPickupLocation(request.getPickupLocation());
        order.setShippingRequirements(request.getShippingRequirements());
        order.setSpecialInstructions(request.getSpecialInstructions());

        order = orderRepository.save(order);
        log.info("Shipment request updated [id={}] by exporter [{}]", order.getId(), exporterEmail);
        return MappingUtil.toOrderResponse(order);
    }

    // ── Logistics operations ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getPendingRequests(String logisticsPartnerEmail) {
        User partner = findUserByEmail(logisticsPartnerEmail);

        // Get all PENDING_LOGISTICS requests, then filter out ones this partner already rejected
        return orderRepository.findByStatus(OrderStatus.PENDING_LOGISTICS)
                .stream()
                .filter(order -> !rejectionRepository.existsByOrderAndLogisticsPartner(order, partner))
                .map(MappingUtil::toOrderResponse)
                .toList();
    }

    @Override
    @Transactional
    public OrderResponse acceptRequest(Long id, String logisticsPartnerEmail) {
        User partner = findUserByEmail(logisticsPartnerEmail);
        Order order = findOrderById(id);

        // Only PENDING_LOGISTICS requests can be accepted
        if (order.getStatus() != OrderStatus.PENDING_LOGISTICS) {
            throw new BadRequestException(
                "This request is no longer available. Current status: " + order.getStatus()
            );
        }

        // Cannot accept a request this partner already rejected
        if (rejectionRepository.existsByOrderAndLogisticsPartner(order, partner)) {
            throw new BadRequestException("You have already rejected this request");
        }

        // Mark order as accepted by this logistics partner
        order.setStatus(OrderStatus.LOGISTICS_ACCEPTED);
        order.setAssignedLogisticsPartner(partner);
        order.setAcceptedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Auto-create the shipment
        String trackingNumber = generateUniqueTrackingNumber();
        String destination = order.getDestinationCountry().getName();

        Shipment shipment = Shipment.builder()
                .order(order)
                .logisticsPartner(partner)
                .trackingNumber(trackingNumber)
                .shipmentStatus(ShipmentStatus.ASSIGNED)
                .origin(order.getPickupLocation())
                .destination(destination)
                .statusUpdatedAt(LocalDateTime.now())
                .build();

        shipmentRepository.save(shipment);

        log.info("Request [id={}] accepted by logistics [{}]. Shipment [{}] auto-created.",
                id, logisticsPartnerEmail, trackingNumber);

        return MappingUtil.toOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse rejectRequest(Long id, String logisticsPartnerEmail) {
        User partner = findUserByEmail(logisticsPartnerEmail);
        Order order = findOrderById(id);

        // Only PENDING_LOGISTICS requests can be rejected
        if (order.getStatus() != OrderStatus.PENDING_LOGISTICS) {
            throw new BadRequestException(
                "This request cannot be rejected. Current status: " + order.getStatus()
            );
        }

        // Prevent duplicate rejections from the same partner
        if (rejectionRepository.existsByOrderAndLogisticsPartner(order, partner)) {
            throw new BadRequestException("You have already rejected this request");
        }

        // Record this partner's rejection
        OrderRejection rejection = OrderRejection.builder()
                .order(order)
                .logisticsPartner(partner)
                .build();
        rejectionRepository.save(rejection);

        // Count total logistics partners in the system
        long totalLogisticsPartners = userRepository.countByRole(Role.LOGISTICS);

        // Count how many have rejected this specific order
        long totalRejections = rejectionRepository.countByOrder(order);

        // If every registered logistics partner has rejected, mark as REJECTED
        if (totalRejections >= totalLogisticsPartners) {
            order.setStatus(OrderStatus.REJECTED);
            orderRepository.save(order);
            log.info("Request [id={}] rejected by all {} logistics partners.", id, totalRejections);
        } else {
            log.info("Request [id={}] rejected by [{}]. Still available to {} other partner(s).",
                    id, logisticsPartnerEmail, totalLogisticsPartners - totalRejections);
        }

        return MappingUtil.toOrderResponse(order);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private void verifyExporterOwnership(Order order, String exporterEmail) {
        if (!order.getExporter().getEmail().equals(exporterEmail)) {
            throw new UnauthorizedException("You do not have permission to access this request");
        }
    }

    private String generateUniqueTrackingNumber() {
        String tracking;
        int attempts = 0;
        do {
            tracking = TrackingNumberUtil.generate();
            attempts++;
            if (attempts > 10) {
                throw new RuntimeException("Unable to generate a unique tracking number");
            }
        } while (shipmentRepository.findByTrackingNumber(tracking).isPresent());
        return tracking;
    }

    private Country resolveDestinationCountry(OrderRequest request) {
        Country destination = null;
        if (request.getDestinationCountryId() != null) {
            destination = countryRepository.findById(request.getDestinationCountryId()).orElse(null);
        }
        if (destination == null && request.getDestinationCountryName() != null && !request.getDestinationCountryName().isBlank()) {
            String name = request.getDestinationCountryName().trim();
            destination = countryRepository.findByNameIgnoreCase(name)
                    .or(() -> countryRepository.findByCodeIgnoreCase(name))
                    .orElse(null);
        }
        if (destination == null && request.getDestinationCountryCode() != null && !request.getDestinationCountryCode().isBlank()) {
            destination = countryRepository.findByCodeIgnoreCase(request.getDestinationCountryCode().trim())
                    .orElse(null);
        }
        if (destination == null) {
            String name = request.getDestinationCountryName();
            if (name != null && !name.isBlank()) {
                String trimmedName = name.trim();
                String code = request.getDestinationCountryCode();
                if (code == null || code.isBlank()) {
                    code = trimmedName.length() >= 2 ? trimmedName.substring(0, 2).toUpperCase() : "XX";
                }
                destination = countryRepository.save(Country.builder()
                        .name(trimmedName)
                        .code(code.trim().toUpperCase())
                        .currency("USD")
                        .active(true)
                        .build());
            } else if (request.getDestinationCountryId() != null) {
                throw new ResourceNotFoundException("Country", "id", request.getDestinationCountryId());
            } else {
                throw new BadRequestException("Destination country ID or Name is required");
            }
        }
        return destination;
    }
}

