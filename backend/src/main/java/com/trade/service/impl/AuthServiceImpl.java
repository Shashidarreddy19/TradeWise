package com.trade.service.impl;

import com.trade.dto.auth.*;
import com.trade.entity.*;
import com.trade.exception.BadRequestException;
import com.trade.exception.FieldValidationException;
import com.trade.exception.ResourceNotFoundException;
import com.trade.repository.*;
import com.trade.security.JwtUtil;
import com.trade.security.UserDetailsServiceImpl;
import com.trade.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Production-grade authentication service.
 *
 * Registration:
 * - Trims and sanitizes all string inputs
 * - Lowercases email before persistence
 * - Normalizes phone to +CC format
 * - Validates GSTIN format when country = India
 * - Prevents duplicate email, phone, and GST registrations
 * - Confirms password match
 * - Validates role-specific required fields
 * - Validates product categories (1–3 selections; "Others" requires description)
 * - Returns structured field-level errors via FieldValidationException
 *
 * Login:
 * - Account lockout after 5 failed attempts (15-minute cooldown)
 * - Resets failure count on successful authentication
 *
 * All passwords are BCrypt-hashed. Plain text is never stored or logged.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final ExporterProfileRepository exporterProfileRepository;
    private final LogisticsProfileRepository logisticsProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;

    // ── Constants ────────────────────────────────────────────────────────────

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private static final Pattern GSTIN_PATTERN = Pattern.compile(
            "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");

    private static final Pattern INDIA_PHONE_PATTERN = Pattern.compile(
            "^(\\+91|91)?[6-9]\\d{9}$");

    private static final Set<String> VALID_BUSINESS_TYPES = Set.of(
            "Manufacturer", "Trader", "Both");

    private static final Set<String> VALID_EXPERIENCE_LEVELS = Set.of(
            "Beginner", "Intermediate", "Experienced");

    private static final Set<String> VALID_CATEGORIES = Set.of(
            "Agricultural Products", "Spices", "Food Products", "Processed Foods",
            "Marine Products", "Textiles", "Apparel & Garments", "Home Textiles",
            "Leather Products", "Footwear", "Handicrafts",
            "Ceramics & Pottery", "Glassware", "Jewellery & Gems",
            "Chemicals", "Cosmetics & Personal Care", "Pharmaceuticals",
            "Plastics & Rubber", "Electronics", "Engineering Goods",
            "Machinery", "Automotive Components", "Furniture & Wood", "Others");

    // ════════════════════════════════════════════════════════════════════════
    // REGISTRATION
    // ════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Sanitize and trim all inputs
        sanitizeRequest(request);

        // 2. Confirm password match
        Map<String, String> errors = new LinkedHashMap<>();
        if (request.getConfirmPassword() == null
                || !request.getPassword().equals(request.getConfirmPassword())) {
            errors.put("confirmPassword", "Passwords do not match.");
        }

        // 3. Duplicate checks
        if (userRepository.existsByEmail(request.getEmail())) {
            errors.put("email", "This email is already registered.");
        }
        String normalizedPhone = normalizePhone(request.getPhone(), request.getCountry());
        if (normalizedPhone != null && !normalizedPhone.isBlank()
                && userRepository.existsByPhone(normalizedPhone)) {
            errors.put("phone", "This phone number is already registered.");
        }

        // 4. Country-specific phone validation
        if ("India".equalsIgnoreCase(request.getCountry())) {
            String raw = (request.getPhone() != null)
                    ? request.getPhone().replaceAll("[\\s\\-]", "") : "";
            if (!INDIA_PHONE_PATTERN.matcher(raw).matches()) {
                errors.put("phone", "Please enter a valid 10-digit Indian mobile number starting with 6, 7, 8 or 9.");
            }
        }

        // 5. Role-specific required fields
        if (request.getRole() == Role.EXPORTER) {
            validateExporterFields(request, errors);
        } else if (request.getRole() == Role.LOGISTICS) {
            if (request.getCompanyName() == null || request.getCompanyName().isBlank()) {
                errors.put("companyName", "Company name is required for Logistics accounts.");
            }
        }

        // 6. Throw all collected errors at once
        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }

        // 7. Build and persist User
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(normalizedPhone)
                .country(request.getCountry())
                .role(request.getRole())
                .failedLoginAttempts(0)
                .build();
        user = userRepository.save(user);

        // 8. Persist role-specific profile
        if (request.getRole() == Role.EXPORTER) {
            ExporterProfile profile = ExporterProfile.builder()
                    .user(user)
                    .companyName(request.getCompanyName())
                    .iecCode(request.getIecCode())
                    .gstNumber(request.getGstNumber())
                    .address(request.getAddress())
                    .businessType(request.getBusinessType())
                    .exportExperience(request.getExportExperience())
                    .productDescription(request.getProductDescription())
                    .productCategories(request.getProductCategories() != null
                            ? String.join(",", request.getProductCategories()) : null)
                    .baseCountry(request.getCountry())
                    .build();
            exporterProfileRepository.save(profile);
        } else {
            LogisticsProfile profile = LogisticsProfile.builder()
                    .user(user)
                    .companyName(request.getCompanyName())
                    .services(request.getServices() != null
                            ? String.join(",", request.getServices()) : null)
                    .serviceArea(request.getServiceArea())
                    .businessRegistrationNumber(request.getBusinessRegistrationNumber())
                    .experience(request.getExperience())
                    .trackingSupport(request.getTrackingSupport() != null
                            ? request.getTrackingSupport() : false)
                    .cargoInsurance(request.getCargoInsurance() != null
                            ? request.getCargoInsurance() : false)
                    .fleetSize(request.getFleetSize())
                    .build();
            logisticsProfileRepository.save(profile);
        }

        log.info("New {} registered: {}", request.getRole(), request.getEmail());

        // 9. Generate JWT
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(normalizeRole(user.getRole()))
                .build();
    }

    // ════════════════════════════════════════════════════════════════════════
    // LOGIN
    // ════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail() != null
                ? request.getEmail().trim().toLowerCase() : "";

        // Check lockout before attempting authentication
        Optional<User> optUser = userRepository.findByEmail(email);
        if (optUser.isPresent()) {
            User u = optUser.get();
            if (u.getLockoutUntil() != null && u.getLockoutUntil().isAfter(LocalDateTime.now())) {
                long minutesLeft = java.time.Duration.between(
                        LocalDateTime.now(), u.getLockoutUntil()).toMinutes() + 1;
                throw new BadRequestException(
                        "Account is locked due to multiple failed login attempts. "
                        + "Please try again in " + minutesLeft + " minute(s).");
            }
        }

        try {
            // Authenticate — throws BadCredentialsException on failure
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
        } catch (BadCredentialsException | LockedException ex) {
            // Increment failed attempts
            optUser.ifPresent(u -> {
                int attempts = (u.getFailedLoginAttempts() != null ? u.getFailedLoginAttempts() : 0) + 1;
                u.setFailedLoginAttempts(attempts);
                if (attempts >= MAX_FAILED_ATTEMPTS) {
                    u.setLockoutUntil(LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
                    log.warn("Account locked for {}: {} failed attempts", email, attempts);
                }
                userRepository.save(u);
            });
            throw new BadCredentialsException("Invalid email or password.");
        }

        // Successful login — reset failure counters
        User user = optUser.orElseThrow(
                () -> new ResourceNotFoundException("User", "email", email));
        if (user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            user.setLockoutUntil(null);
            userRepository.save(user);
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        log.info("User logged in: {}", email);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(normalizeRole(user.getRole()))
                .build();
    }

    // ════════════════════════════════════════════════════════════════════════
    // PROFILE
    // ════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        UserProfileResponse.UserProfileResponseBuilder builder = UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .createdAt(user.getCreatedAt());

        if (user.getRole() == Role.EXPORTER || user.getRole() == Role.IMPORTER
                || user.getRole() == Role.ADMIN) {
            exporterProfileRepository.findByUser(user).ifPresent(profile -> {
                builder.companyName(profile.getCompanyName());
                builder.iecCode(profile.getIecCode());
                builder.gstNumber(profile.getGstNumber());
                builder.address(profile.getAddress());
            });
        } else {
            // LOGISTICS or LOGISTICS_PARTNER
            logisticsProfileRepository.findByUser(user).ifPresent(profile -> {
                builder.companyName(profile.getCompanyName());
                builder.serviceArea(profile.getServiceArea());
                builder.fleetSize(profile.getFleetSize());
                builder.services(profile.getServices());
                builder.businessRegistrationNumber(profile.getBusinessRegistrationNumber());
                builder.experience(profile.getExperience());
                builder.trackingSupport(profile.getTrackingSupport());
                builder.cargoInsurance(profile.getCargoInsurance());
            });
        }

        return builder.build();
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(ProfileUpdateRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName().trim());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.setPhone(request.getPhone().trim());
        }
        userRepository.save(user);

        if (user.getRole() == Role.LOGISTICS) {
            LogisticsProfile profile = logisticsProfileRepository.findByUser(user)
                    .orElseGet(() -> LogisticsProfile.builder().user(user).build());

            if (request.getCompanyName() != null) profile.setCompanyName(request.getCompanyName().trim());
            if (request.getServiceArea() != null) profile.setServiceArea(request.getServiceArea().trim());
            if (request.getServices() != null) {
                profile.setServices(String.join(", ", request.getServices()));
            }
            if (request.getBusinessRegistrationNumber() != null) {
                profile.setBusinessRegistrationNumber(request.getBusinessRegistrationNumber().trim().toUpperCase());
            }
            if (request.getExperience() != null) profile.setExperience(request.getExperience().trim());
            if (request.getTrackingSupport() != null) profile.setTrackingSupport(request.getTrackingSupport());
            if (request.getCargoInsurance() != null) profile.setCargoInsurance(request.getCargoInsurance());

            logisticsProfileRepository.save(profile);
            log.info("Logistics profile updated for [{}]", email);
        } else if (user.getRole() == Role.EXPORTER) {
            ExporterProfile profile = exporterProfileRepository.findByUser(user)
                    .orElseGet(() -> ExporterProfile.builder().user(user).build());

            if (request.getCompanyName() != null) profile.setCompanyName(request.getCompanyName().trim());
            if (request.getAddress() != null) profile.setAddress(request.getAddress().trim());
            if (request.getGstNumber() != null) profile.setGstNumber(request.getGstNumber().trim().toUpperCase());
            if (request.getIecCode() != null) profile.setIecCode(request.getIecCode().trim());
            if (request.getBusinessType() != null) profile.setBusinessType(request.getBusinessType());
            if (request.getExportExperience() != null) profile.setExportExperience(request.getExportExperience());
            if (request.getProductDescription() != null) profile.setProductDescription(request.getProductDescription());

            exporterProfileRepository.save(profile);
            log.info("Exporter profile updated for [{}]", email);
        }

        return getProfile(email);
    }

    // ════════════════════════════════════════════════════════════════════════
    // DUPLICATE-CHECK HELPERS (used by AuthController for real-time checks)
    // ════════════════════════════════════════════════════════════════════════

    @Override
    public boolean isEmailAvailable(String email) {
        if (email == null || email.isBlank()) return true;
        return !userRepository.existsByEmail(email.trim().toLowerCase());
    }

    @Override
    public boolean isPhoneAvailable(String phone) {
        if (phone == null || phone.isBlank()) return true;
        String trimmed = phone.trim();
        if (userRepository.existsByPhone(trimmed)) return false;
        String indiaNormalized = normalizePhone(trimmed, "India");
        return indiaNormalized == null || !userRepository.existsByPhone(indiaNormalized);
    }

    // ════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Trim whitespace, collapse multiple spaces, lowercase email, strip HTML tags.
     */
    private void sanitizeRequest(RegisterRequest req) {
        req.setName(sanitize(req.getName()));
        req.setCompanyName(sanitize(req.getCompanyName()));
        req.setEmail(req.getEmail() != null ? req.getEmail().trim().toLowerCase() : null);
        req.setPhone(req.getPhone() != null ? req.getPhone().trim().replaceAll("[\\s\\-]", "") : null);
        req.setCountry(sanitize(req.getCountry()));
        req.setGstNumber(req.getGstNumber() != null ? req.getGstNumber().trim().toUpperCase() : null);
        req.setProductDescription(sanitize(req.getProductDescription()));
        req.setOtherCategoryDescription(sanitize(req.getOtherCategoryDescription()));
        req.setBusinessType(sanitize(req.getBusinessType()));
        req.setExportExperience(sanitize(req.getExportExperience()));
    }

    /**
     * Trim, collapse consecutive spaces, and strip any HTML/script tags (XSS prevention).
     */
    private String sanitize(String input) {
        if (input == null) return null;
        // Strip HTML tags
        String cleaned = input.replaceAll("<[^>]*>", "");
        // Collapse multiple spaces
        cleaned = cleaned.trim().replaceAll("\\s{2,}", " ");
        return cleaned.isEmpty() ? null : cleaned;
    }

    /**
     * Normalize phone to +CountryCode format for consistent storage and lookup.
     */
    private String normalizePhone(String phone, String country) {
        if (phone == null || phone.isBlank()) return null;
        String digits = phone.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+")) return digits;
        // India: prefix +91
        if ("India".equalsIgnoreCase(country)) {
            if (digits.startsWith("91") && digits.length() == 12) return "+" + digits;
            if (digits.startsWith("0")) digits = digits.substring(1);
            if (digits.length() == 10) return "+91" + digits;
        }
        // Other countries: store as-is with + prefix if it looks like a full number
        if (digits.length() >= 7) return "+" + digits;
        return digits;
    }

    /**
     * Validate exporter-specific fields: GST, business type, categories, product description.
     */
    private void validateExporterFields(RegisterRequest request, Map<String, String> errors) {
        // Company name
        if (request.getCompanyName() == null || request.getCompanyName().isBlank()) {
            errors.put("companyName", "Company name is required for Exporter accounts.");
        }

        // GST / Business Registration Number
        if (request.getGstNumber() == null || request.getGstNumber().isBlank()) {
            errors.put("gstNumber", "Please enter a valid GST or Business Registration Number.");
        } else {
            // India-specific GSTIN validation
            if ("India".equalsIgnoreCase(request.getCountry())) {
                if (!GSTIN_PATTERN.matcher(request.getGstNumber()).matches()) {
                    errors.put("gstNumber", "Please enter a valid 15-character GSTIN (e.g. 29AAAAA0000A1Z1).");
                }
            } else {
                // International: 5–30 alphanumeric
                if (request.getGstNumber().length() < 5 || request.getGstNumber().length() > 30) {
                    errors.put("gstNumber", "Business Registration Number must be 5–30 characters.");
                }
            }
            // Duplicate GST check
            if (!errors.containsKey("gstNumber")
                    && exporterProfileRepository.existsByGstNumber(request.getGstNumber())) {
                errors.put("gstNumber", "This GST/Business Registration Number is already registered.");
            }
        }

        // Business Type
        if (request.getBusinessType() == null || request.getBusinessType().isBlank()) {
            errors.put("businessType", "Please select a business type.");
        } else if (!VALID_BUSINESS_TYPES.contains(request.getBusinessType())) {
            errors.put("businessType", "Business type must be Manufacturer, Trader, or Both.");
        }

        // Product Categories (1–3 required)
        List<String> categories = request.getProductCategories();
        if (categories == null || categories.isEmpty()) {
            errors.put("productCategories", "Please select at least one product category.");
        } else if (categories.size() > 3) {
            errors.put("productCategories", "You may select a maximum of 3 product categories.");
        } else {
            for (String cat : categories) {
                if (!VALID_CATEGORIES.contains(cat)) {
                    errors.put("productCategories", "Invalid category: " + cat);
                    break;
                }
            }
            // If "Others" selected, the description field is mandatory
            if (categories.contains("Others")) {
                if (request.getOtherCategoryDescription() == null
                        || request.getOtherCategoryDescription().length() < 3) {
                    errors.put("otherCategoryDescription",
                            "Please specify the product category (minimum 3 characters).");
                }
            }
        }

        // Product Description (10–500 chars)
        if (request.getProductDescription() == null
                || request.getProductDescription().length() < 10) {
            errors.put("productDescription",
                    "Please describe your primary products (minimum 10 characters).");
        } else if (request.getProductDescription().length() > 500) {
            errors.put("productDescription",
                    "Product description must not exceed 500 characters.");
        }

        // Export Experience
        if (request.getExportExperience() == null || request.getExportExperience().isBlank()) {
            errors.put("exportExperience", "Please select your export experience level.");
        } else if (!VALID_EXPERIENCE_LEVELS.contains(request.getExportExperience())) {
            errors.put("exportExperience", "Export experience must be Beginner, Intermediate, or Experienced.");
        }
    }

    /**
     * Normalize legacy DB roles to canonical EXPORTER / LOGISTICS values.
     * Ensures the frontend always receives a predictable role string.
     */
    private Role normalizeRole(Role role) {
        return switch (role) {
            case LOGISTICS_PARTNER -> Role.LOGISTICS;
            case IMPORTER, ADMIN   -> Role.EXPORTER;
            default                -> role;
        };
    }
}
