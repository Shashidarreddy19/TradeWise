package com.trade.service;

import com.trade.dto.auth.AuthResponse;
import com.trade.dto.auth.LoginRequest;
import com.trade.dto.auth.ProfileUpdateRequest;
import com.trade.dto.auth.RegisterRequest;
import com.trade.dto.auth.UserProfileResponse;

/**
 * Contract for authentication and user profile operations.
 */
public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserProfileResponse getProfile(String email);

    UserProfileResponse updateProfile(ProfileUpdateRequest request, String email);

    boolean isEmailAvailable(String email);

    boolean isPhoneAvailable(String phone);
}
