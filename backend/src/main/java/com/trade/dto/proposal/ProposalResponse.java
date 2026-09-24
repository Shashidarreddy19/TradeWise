package com.trade.dto.proposal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.trade.entity.ProposalStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detailed representation of a logistics proposal returned to Exporter or Logistics provider.
 */
@Data
@Builder
public class ProposalResponse {

    private Long id;
    private Long orderId;
    private String orderProductName;
    private String orderDestinationCountry;
    private Integer orderQuantity;

    // Logistics Provider details
    private Long logisticsPartnerId;
    private String logisticsPartnerName;
    private String logisticsPartnerCompany;
    private String logisticsPartnerPhone;
    private String logisticsPartnerEmail;
    private String logisticsPartnerExperience;
    private String logisticsPartnerServiceArea;
    private Boolean trackingSupport;
    private Boolean cargoInsurance;

    // Quotation details
    private List<String> services;
    private BigDecimal estimatedCost;
    private String currency;
    private Integer estimatedTransitDays;
    private LocalDate pickupDate;
    private LocalDate expectedDeliveryDate;
    private BigDecimal additionalCharges;
    private BigDecimal totalAmount;
    private String notes;
    private ProposalStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Frontend compatibility aliases (same values as canonical fields). */
    @JsonProperty("proposedCost")
    public BigDecimal getProposedCost() {
        return estimatedCost;
    }

    @JsonProperty("offeredServices")
    public List<String> getOfferedServices() {
        return services;
    }

    @JsonProperty("estimatedPickupDate")
    public LocalDate getEstimatedPickupDate() {
        return pickupDate;
    }

    @JsonProperty("estimatedDeliveryDate")
    public LocalDate getEstimatedDeliveryDate() {
        return expectedDeliveryDate;
    }

    @JsonProperty("carrierCompany")
    public String getCarrierCompany() {
        return logisticsPartnerCompany;
    }

    @JsonProperty("carrierName")
    public String getCarrierName() {
        return logisticsPartnerName;
    }

    @JsonProperty("carrierId")
    public Long getCarrierId() {
        return logisticsPartnerId;
    }

    @JsonProperty("carrierEmail")
    public String getCarrierEmail() {
        return logisticsPartnerEmail;
    }

    @JsonProperty("carrierPhone")
    public String getCarrierPhone() {
        return logisticsPartnerPhone;
    }
}
