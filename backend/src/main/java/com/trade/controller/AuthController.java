package com.trade.controller;

import com.trade.dto.ApiResponse;
import com.trade.dto.auth.AuthResponse;
import com.trade.dto.auth.LoginRequest;
import com.trade.dto.auth.RegisterRequest;
import com.trade.dto.auth.UserProfileResponse;
import com.trade.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Handles user registration, login, and profile retrieval.
 *
 * Public endpoints:
 *   POST /api/auth/register
 *   POST /api/auth/login
 *
 * Protected endpoint:
 *   GET  /api/auth/profile  (requires Bearer token)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new user (EXPORTER or LOGISTICS).
     * Returns a JWT on success.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account registered successfully", response));
    }

    /**
     * Authenticate an existing user.
     * Returns a JWT on success.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    /**
     * Get the full profile of the currently authenticated user.
     * Requires a valid Bearer token in the Authorization header.
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {

        UserProfileResponse profile = authService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    // ── Real-time duplicate detection endpoints ──────────────────────────────

    /**
     * Check if an email address is available for registration.
     * GET /api/auth/check-email?email=foo@bar.com
     */
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestParam String email) {
        boolean available = ((com.trade.service.impl.AuthServiceImpl) authService)
                .isEmailAvailable(email);
        return ResponseEntity.ok(Map.of(
                "available", available,
                "message", available ? "Email is available." : "This email is already registered."
        ));
    }

    /**
     * Check if a phone number is available for registration.
     * GET /api/auth/check-phone?phone=+919876543210
     */
    @GetMapping("/check-phone")
    public ResponseEntity<Map<String, Object>> checkPhone(@RequestParam String phone) {
        boolean available = ((com.trade.service.impl.AuthServiceImpl) authService)
                .isPhoneAvailable(phone);
        return ResponseEntity.ok(Map.of(
                "available", available,
                "message", available ? "Phone number is available." : "This phone number is already registered."
        ));
    }
}
