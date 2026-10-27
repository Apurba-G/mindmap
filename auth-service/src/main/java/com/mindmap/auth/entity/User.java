package com.mindmap.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Core User entity mapped to auth_schema.users table.
 * Supports Argon2id password & recovery hashes, account lockout tracking,
 * and computed lowercase user ID for case-insensitive uniqueness.
 */
@Entity
@Table(
    name = "users",
    schema = "auth_schema",
    indexes = {
        @Index(name = "idx_users_user_id_lower", columnList = "user_id_lower", unique = true),
        @Index(name = "idx_users_is_active", columnList = "is_active")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true, length = 50)
    private String userId;

    @Column(name = "user_id_lower", nullable = false, unique = true, length = 50)
    private String userIdLower;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "recovery_hash", nullable = false, length = 255)
    private String recoveryHash;

    @Builder.Default
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = false;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Builder.Default
    @Column(name = "is_admin_seeded", nullable = false)
    private boolean isAdminSeeded = false;

    @Builder.Default
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_roles",
        schema = "auth_schema",
        joinColumns = @JoinColumn(name = "user_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Set<Role> roles = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        computeUserIdLower();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
        computeUserIdLower();
    }

    private void computeUserIdLower() {
        if (this.userId != null) {
            this.userIdLower = this.userId.trim().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * Checks if the user account is currently locked due to failed attempts.
     */
    public boolean isAccountLocked() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }

    /**
     * Adds an authorization role.
     */
    public void addRole(Role role) {
        if (this.roles == null) {
            this.roles = new HashSet<>();
        }
        this.roles.add(role);
    }

    /**
     * Checks if the user has the specified role.
     */
    public boolean hasRole(Role role) {
        return roles != null && roles.contains(role);
    }
}
