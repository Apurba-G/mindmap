package com.mindmap.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for tracking authentication attempts for brute-force protection and audits.
 */
@Entity
@Table(
    name = "login_attempts",
    schema = "auth_schema",
    indexes = {
        @Index(name = "idx_login_attempts_user_id_time", columnList = "user_id, attempted_at"),
        @Index(name = "idx_login_attempts_ip_time", columnList = "ip_address, attempted_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "successful", nullable = false)
    private boolean successful;

    @Column(name = "failure_reason", length = 100)
    private String failureReason;

    @Column(name = "attempted_at", nullable = false, updatable = false)
    private Instant attemptedAt;

    @PrePersist
    protected void onCreate() {
        if (this.attemptedAt == null) {
            this.attemptedAt = Instant.now();
        }
    }
}
