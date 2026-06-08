package com.orderflux.backend.model;

import com.orderflux.backend.model.base.BaseEntity;
import com.orderflux.backend.model.enums.Role;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_users_email",
            columnNames = "email"
        ),
        @UniqueConstraint(
            name = "uk_users_phone_number",
            columnNames = "phone_number"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Personal Info ─────────────────────────────────────────
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    // ── Contact & Auth ────────────────────────────────────────
    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    /**
     * phoneNumber:
     *   nullable = true (no nullable=false) → existing users unaffected
     *   Uniqueness enforced via @Table uniqueConstraints above
     *   Validation (required/format) handled at DTO level
     */
    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    // ── Role ──────────────────────────────────────────────────
    /**
     * EnumType.STRING — stores "ROLE_CUSTOMER" not 0
     * Never use ORDINAL — enum reordering silently corrupts data
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    // ── Status Flags ──────────────────────────────────────────
    /**
     * isEnabled:
     *   Controlled by ADMIN — can disable any account
     *   Independent of email verification
     */
    @Builder.Default
    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    /**
     * isLocked:
     *   Triggered by security events (too many failed logins)
     *   Future: auto-lock after N failed attempts
     */
    @Builder.Default
    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;

    /**
     * isEmailVerified:
     *   false on registration
     *   true ONLY after OTP verification succeeds
     *   Login blocked until this is true
     */
    @Builder.Default
    @Column(name = "is_email_verified", nullable = false)
    private Boolean isEmailVerified = false;
}