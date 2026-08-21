package com.trade.dto.shipment;

import com.trade.entity.ShipmentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for a shipment record.
 */
@Data
@Builder
public class ShipmentResponse {

    private Long id;

    // Linked order
    private Long orderId;
    private String orderProductName;
    private String orderDestinationCountry;
    private Integer orderQuantity;

    // Exporter info
    private Long exporterId;
    private String exporterName;
    private String exporterCompany;

    // Logistics partner
    private Long logisticsPartnerId;
    private String logisticsPartnerName;

    // Shipment tracking
    private String trackingNumber;
    private ShipmentStatus shipmentStatus;
    private String origin;
    private String destination;
    private LocalDate estimatedDelivery;
    private LocalDateTime statusUpdatedAt;
    private LocalDateTime createdAt;
}
