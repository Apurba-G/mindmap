package com.mindmap.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Locale;

/**
 * Entity representing system-reserved usernames that cannot be claimed by users.
 */
@Entity
@Table(
    name = "reserved_usernames",
    schema = "auth_schema",
    indexes = {
        @Index(name = "idx_reserved_usernames_word", columnList = "word", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservedUsername {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "word", nullable = false, unique = true, length = 50)
    private String word;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.word != null) {
            this.word = this.word.trim().toLowerCase(Locale.ROOT);
        }
    }
}
