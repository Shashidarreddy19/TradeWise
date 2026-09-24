package com.trade.dto.shipment;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trade.entity.ShipmentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for a shipment record, including financial quote info,
 * logistics partner details, and historical tracking milestones.
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
    private String logisticsPartnerCompany;

    // Shipment tracking & logistics terms
    private String trackingNumber;
    private ShipmentStatus shipmentStatus;
    private String origin;
    private String destination;
    private String services;
    private BigDecimal cost;
    private String currency;
    private LocalDate pickupDate;
    private LocalDate estimatedDelivery;
    private LocalDateTime statusUpdatedAt;
    private LocalDateTime createdAt;

    // Historical tracking timeline
    private List<ShipmentTrackingResponse> trackingHistory;

    @JsonProperty("carrierName")
    public String getCarrierName() {
        return logisticsPartnerName;
    }

    @JsonProperty("carrierCompany")
    public String getCarrierCompany() {
        return logisticsPartnerCompany;
    }
}
