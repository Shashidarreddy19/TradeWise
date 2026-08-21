package com.trade.dto.order;

import com.trade.entity.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for an Export Shipment Request.
 */
@Data
@Builder
public class OrderResponse {

    private Long id;

    // Product details
    private Long productId;
    private String productName;
    private String productHsCode;
    private BigDecimal productWeightPerUnit;

    // Exporter details
    private Long exporterId;
    private String exporterName;
    private String exporterCompany;

    // Destination
    private Long destinationCountryId;
    private String destinationCountryName;

    // Request details
    private Integer quantity;
    private BigDecimal totalPrice;
    private String pickupLocation;
    private String shippingRequirements;
    private String specialInstructions;

    // Status
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime acceptedAt;

    // Assigned logistics partner (null until accepted)
    private Long assignedLogisticsPartnerId;
    private String assignedLogisticsPartnerName;
    private String assignedLogisticsPartnerCompany;
}
