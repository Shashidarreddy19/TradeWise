package com.trade.dto.auth;

import com.trade.entity.Role;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

/**
 * Request payload for user registration.
 *
 * Every field carries Jakarta Validation annotations that mirror the frontend
 * rules. The backend always re-validates — never trust the client.
 *
 * Field-level error messages are user-facing and professional.
 */
@Data
public class RegisterRequest {

    // ════════════════════════════════════════════════════════════════════════
    // STEP 1 — BASIC INFORMATION
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Full Name: 3–60 chars, only letters, spaces, apostrophe, period.
     */
    @NotBlank(message = "Please enter your full name.")
    @Size(min = 3, max = 60, message = "Full name must be between 3 and 60 characters.")
    @Pattern(
        regexp = "^[A-Za-z][A-Za-z '.\\-]{1,58}[A-Za-z.]$",
        message = "Full name may only contain letters, spaces, apostrophe (') and period (.)."
    )
    private String name;

    /**
     * Company Name: 2–100 chars, letters, numbers, spaces, &, -, period.
     */
    @NotBlank(message = "Company name is required.")
    @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters.")
    @Pattern(
        regexp = "^[A-Za-z0-9][A-Za-z0-9 &.\\-]{0,98}[A-Za-z0-9.&]$",
        message = "Company name may only contain letters, numbers, spaces, &, - and period."
    )
    private String companyName;

    /**
     * Email: valid RFC-5322 format. Lowercased and trimmed before persistence.
     */
    @NotBlank(message = "Email address is required.")
    @Email(message = "Please enter a valid email address.")
    @Size(max = 120, message = "Email must not exceed 120 characters.")
    private String email;

    /**
     * Phone number: validated per-country in the service layer.
     * The stored format is always +CountryCodeDigits (e.g. +919876543210).
     */
    @NotBlank(message = "Phone number is required.")
    @Size(min = 7, max = 15, message = "Phone number must be between 7 and 15 digits.")
    @Pattern(
        regexp = "^\\+?[0-9]{7,15}$",
        message = "Please enter a valid phone number (digits only, optional leading +)."
    )
    private String phone;

    /**
     * Base Country: ISO-3166 country name or code.
     */
    @NotBlank(message = "Please select your base country.")
    private String country;

    /**
     * Password: 8–64 chars, must contain uppercase, lowercase, digit, special char.
     * Pattern enforces composition; BCrypt hash is stored, never the plain value.
     */
    @NotBlank(message = "Password is required.")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters.")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?~`])[A-Za-z\\d!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?~`]{8,64}$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character."
    )
    private String password;

    /**
     * Confirm password. Compared in the service layer (not a DB field).
     */
    @NotBlank(message = "Please confirm your password.")
    private String confirmPassword;

    /**
     * Account Role: exactly one of EXPORTER or LOGISTICS.
     */
    @NotNull(message = "Please select an account role.")
    private Role role;

    // ════════════════════════════════════════════════════════════════════════
    // STEP 2 — EXPORTER-SPECIFIC FIELDS (required when role = EXPORTER)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * GST Number (India) or Business Registration Number (international).
     * GSTIN format: 2-digit state + 10-char PAN + 1 entity + Z + checksum.
     * For non-IN countries: 5–30 alphanumeric chars.
     * Validated per-country in the service layer.
     */
    @Size(min = 5, max = 30, message = "Please enter a valid GST or Business Registration Number (5–30 characters).")
    private String gstNumber;

    /**
     * Business Type: one of Manufacturer, Trader, Both.
     */
    private String businessType;

    /**
     * Product Categories: 1–3 selections from the defined list.
     * Stored as JSON array in the database.
     */
    @Size(max = 3, message = "You may select a maximum of 3 product categories.")
    private List<String> productCategories;

    /**
     * If "Others" is among productCategories, this field must be filled.
     * 3–50 characters.
     */
    @Size(min = 3, max = 50, message = "Please specify the product category (3–50 characters).")
    private String otherCategoryDescription;

    /**
     * Primary Product Description: 10–500 characters.
     */
    @Size(min = 10, max = 500, message = "Product description must be between 10 and 500 characters.")
    private String productDescription;

    /**
     * Export Experience: Beginner, Intermediate, or Experienced.
     */
    private String exportExperience;

    // ════════════════════════════════════════════════════════════════════════
    // STEP 2 — LOGISTICS-SPECIFIC FIELDS (required when role = LOGISTICS)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Service types offered (1-4 selections).
     */
    @Size(max = 4, message = "You can select up to 4 services.")
    private List<String> services;

    /**
     * Service regions covered (1-5 selections).
     */
    @Size(max = 5, message = "You can select up to 5 regions.")
    private List<String> regions;

    /**
     * Business Registration Number for logistics partner.
     */
    @Size(min = 8, max = 20, message = "Enter a valid Business Registration Number (8-20 characters).")
    private String businessRegistrationNumber;

    /**
     * Years of experience in logistics.
     */
    private String experience;

    /**
     * Whether live tracking is offered.
     */
    private Boolean trackingSupport;

    /**
     * Whether cargo insurance is offered.
     */
    private Boolean cargoInsurance;

    /**
     * Comma-separated service regions (legacy, derived from regions list).
     */
    private String serviceArea;

    /**
     * Fleet size description (legacy).
     */
    private String fleetSize;

    // ════════════════════════════════════════════════════════════════════════
    // LEGACY FIELDS (retained for backward compatibility with older clients)
    // ════════════════════════════════════════════════════════════════════════

    private String iecCode;
    private String address;
}