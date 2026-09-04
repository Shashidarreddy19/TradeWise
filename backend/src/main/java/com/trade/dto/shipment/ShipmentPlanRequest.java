package com.trade.dto.shipment;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ShipmentPlanRequest {

    @NotBlank(message = "HS Code is required")
    private String hsCode;

    @NotBlank(message = "Product name is required")
    private String productName;

    @NotBlank(message = "Origin location is required")
    private String originLocation;

    @NotBlank(message = "Destination country is required")
    private String destinationCountry;

    private String destinationCity;
    private String pickupAddress;

    @NotBlank(message = "Incoterm is required")
    private String incoterm; // EXW, FCA, FOB, CFR, CIF, DAP, DDP, DPU

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Weight per unit is required")
    private BigDecimal weightPerUnitKg;

    // Dimensions per unit in cm (optional or default)
    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;

    private String containerPreference; // 20FT, 40FT, 40HC, LCL, FCL, REEFER, OPEN_TOP, FLAT_RACK, TANK
    private Boolean isDangerousGoods;
    private String temperatureRequirement;

    private LocalDate expectedDispatchDate;
    private LocalDate expectedDeliveryDate;

    private String preferredTransportMode; // ROAD, RAIL, AIR, SEA, MULTIMODAL
}
