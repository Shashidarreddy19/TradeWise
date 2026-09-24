package com.trade.dto.shipment;

import com.trade.entity.ShipmentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Event entry in a shipment's tracking timeline.
 */
@Data
@Builder
public class ShipmentTrackingResponse {

    private Long id;
    private Long shipmentId;
    private ShipmentStatus status;
    private String statusLabel;
    private String location;
    private String description;
    private LocalDateTime createdAt;
}
