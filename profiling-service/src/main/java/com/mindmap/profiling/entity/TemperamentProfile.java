package com.mindmap.profiling.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Persisted Level 3 Temperament breakdown and classification.
 */
@Entity
@Table(name = "temperament_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemperamentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false, unique = true)
    private UserProfile profile;

    @Column(name = "strong_calm_pct", nullable = false)
    private Double strongCalmPct;

    @Column(name = "moderate_temper_pct", nullable = false)
    private Double moderateTemperPct;

    @Column(name = "low_control_angry_pct", nullable = false)
    private Double lowControlAngryPct;

    @Column(name = "low_control_confused_pct", nullable = false)
    private Double lowControlConfusedPct;

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_type", nullable = false)
    private TemperamentType primaryType;

    @Column(name = "explanation_text", nullable = false, columnDefinition = "TEXT")
    private String explanationText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
