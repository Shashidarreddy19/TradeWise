package com.trade.dto.shipment;

import com.trade.entity.ShipmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request payload for updating the status of an existing shipment.
 */
@Data
public class ShipmentStatusRequest {

    @NotNull(message = "Shipment status is required")
    private ShipmentStatus shipmentStatus;
}
