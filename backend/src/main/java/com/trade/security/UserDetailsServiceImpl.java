package com.trade.security;

import com.trade.entity.User;
import com.trade.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Loads user-specific data for Spring Security authentication.
 * Uses the email address as the username identifier.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email));

        // Normalize legacy DB roles to canonical Spring Security roles.
        // LOGISTICS_PARTNER in DB is treated as LOGISTICS throughout the application.
        String roleName = switch (user.getRole()) {
            case LOGISTICS_PARTNER -> "LOGISTICS";
            case IMPORTER          -> "EXPORTER"; // IMPORTER has same access as EXPORTER
            case ADMIN             -> "EXPORTER"; // ADMIN fallback
            default                -> user.getRole().name();
        };

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + roleName)))
                .build();
    }
}
