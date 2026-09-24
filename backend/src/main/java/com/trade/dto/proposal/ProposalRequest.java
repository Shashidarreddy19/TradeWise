package com.trade.dto.proposal;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Payload sent by a logistics provider when quoting on an export order.
 */
@Data
public class ProposalRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotEmpty(message = "At least one service must be selected")
    @JsonAlias({"offeredServices", "serviceList"})
    private List<String> services;

    @NotNull(message = "Estimated cost is required")
    @DecimalMin(value = "0.01", message = "Estimated cost must be greater than 0")
    @JsonAlias({"proposedCost", "cost", "totalCost"})
    private BigDecimal estimatedCost;

    private String currency = "INR";

    @JsonAlias({"transitDays", "days"})
    private Integer estimatedTransitDays;

    @JsonAlias({"estimatedPickupDate", "pickup"})
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate pickupDate;

    @JsonAlias({"estimatedDeliveryDate", "deliveryDate", "delivery"})
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expectedDeliveryDate;

    private BigDecimal additionalCharges = BigDecimal.ZERO;

    private String notes;
}
