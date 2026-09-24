package com.trade.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * Payload for updating user & company profile.
 */
@Data
public class ProfileUpdateRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String phone;

    private String companyName;

    private String address;

    // Logistics specific
    private String businessRegistrationNumber;

    private String experience;

    private String serviceArea;

    private List<String> services;

    private Boolean trackingSupport;

    private Boolean cargoInsurance;

    // Exporter specific
    private String gstNumber;

    private String iecCode;

    private String businessType;

    private String exportExperience;

    private String productDescription;
}
