package com.trade.dto.auth;

import com.trade.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Full profile response returned by GET /api/auth/profile.
 */
@Data
@Builder
public class UserProfileResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private Role role;
    private LocalDateTime createdAt;

    // Exporter profile fields (null if role is LOGISTICS)
    private String companyName;
    private String iecCode;
    private String gstNumber;
    private String address;

    // Logistics profile fields (null if role is EXPORTER)
    private String serviceArea;
    private String fleetSize;
}
