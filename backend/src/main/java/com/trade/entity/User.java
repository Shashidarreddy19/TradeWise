package com.trade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a registered user of the Trade platform.
 * A user can be either an EXPORTER or a LOGISTICS partner.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String password;

    @Column(unique = true)
    private String phone;

    /**
     * Base country (ISO name, e.g. "India", "Germany").
     */
    @Column
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Role role;

    // ── Account Lockout ──────────────────────────────────────────────────

    /**
     * Number of consecutive failed login attempts since last success.
     */
    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    /**
     * If non-null and in the future, the account is locked until this time.
     */
    @Column(name = "lockout_until")
    private LocalDateTime lockoutUntil;

    // ── Timestamps ───────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Relationships ────────────────────────────────────────────────────

    // One user has one exporter profile (null if LOGISTICS)
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ExporterProfile exporterProfile;

    // One user has one logistics profile (null if EXPORTER)
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private LogisticsProfile logisticsProfile;
}
