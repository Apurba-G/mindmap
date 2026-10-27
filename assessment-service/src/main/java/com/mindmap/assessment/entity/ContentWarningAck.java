package com.mindmap.assessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity tracking user acknowledgment/consent for sensitive assessment content warnings.
 */
@Entity
@Table(
    name = "content_warning_acks",
    schema = "assessment_schema",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_level_part", columnNames = {"user_id", "level", "part"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentWarningAck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", length = 32, nullable = false)
    private AssessmentLevel level;

    @Enumerated(EnumType.STRING)
    @Column(name = "part", length = 32, nullable = false)
    private AssessmentPart part;

    @Column(name = "accepted", nullable = false)
    @Builder.Default
    private boolean accepted = false;

    @CreationTimestamp
    @Column(name = "acked_at", nullable = false, updatable = false)
    private LocalDateTime ackedAt;
}
