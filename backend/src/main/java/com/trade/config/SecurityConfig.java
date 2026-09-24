package com.trade.config;

import com.trade.security.JwtAccessDeniedHandler;
import com.trade.security.JwtAuthenticationEntryPoint;
import com.trade.security.JwtAuthenticationFilter;
import com.trade.security.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Security configuration.
 *
 * Role rules:
 *  EXPORTER  — products, create/view own orders, market-analysis, exporter dashboard
 *  LOGISTICS — pending requests, accept/reject orders, shipments, logistics dashboard
 *
 * Note: /api/orders is split by method-level @PreAuthorize in OrderController,
 *       so the URL-level rule here just requires authentication for /api/orders/**
 *       and lets method-level annotations enforce the role per endpoint.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    @Value("${app.cors.allowed-origins:}")
    private String extraAllowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            UserDetailsServiceImpl userDetailsService,
            JwtAuthenticationEntryPoint authenticationEntryPoint,
            JwtAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                // Public
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/health", "/health", "/actuator/health").permitAll()
                .requestMatchers("/api/countries").permitAll()
                .requestMatchers("/api/categories").permitAll()

                // Regulatory Intelligence APIs (authenticated — any role)
                .requestMatchers("/api/v1/**").authenticated()

                // Products — EXPORTER only (method-level annotation backs this up)
                .requestMatchers("/api/products/**").hasRole("EXPORTER")

                // Orders — both EXPORTER and LOGISTICS access different sub-paths;
                // method-level @PreAuthorize in OrderController enforces role per endpoint
                .requestMatchers("/api/orders/**").authenticated()

                // Market analysis — EXPORTER only
                .requestMatchers("/api/market-analysis").hasRole("EXPORTER")

                // Logistics Shipment Planning APIs (authenticated — any role)
                .requestMatchers("/api/shipment/**").authenticated()

                // Shipments — EXPORTER (own) and LOGISTICS (assigned); method-level security enforces role
                .requestMatchers("/api/shipments/**").authenticated()

                // Logistics quotes / proposals
                .requestMatchers("/api/proposals/**").authenticated()

                // Dashboards
                .requestMatchers("/api/dashboard/exporter").hasRole("EXPORTER")
                .requestMatchers("/api/dashboard/logistics").hasRole("LOGISTICS")

                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(ex -> ex
                // 401 — no token or invalid/expired token
                .authenticationEntryPoint(authenticationEntryPoint)
                // 403 — valid token but wrong role
                .accessDeniedHandler(accessDeniedHandler)
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = new ArrayList<>(List.of(
            "http://localhost:[*]",
            "http://localhost:*",
            "http://127.0.0.1:[*]",
            "http://127.0.0.1:*",
            "https://*.vercel.app",
            "https://*.netlify.app",
            "https://*.onrender.com",
            "https://*.koyeb.app",
            "https://*.railway.app"
        ));
        if (extraAllowedOrigins != null && !extraAllowedOrigins.isBlank()) {
            for (String origin : extraAllowedOrigins.split(",")) {
                if (!origin.isBlank()) {
                    origins.add(origin.trim());
                }
            }
        }
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Link", "X-Total-Count"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
