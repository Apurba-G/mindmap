package com.mindmap.assessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Psychological trait definition referenced by questions and scoring mechanisms.
 */
@Entity
@Table(name = "trait_definitions", schema = "assessment_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TraitDefinition {

    @Id
    @Column(name = "key", length = 64, nullable = false)
    private String key;

    @Column(name = "display_name", length = 128, nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", length = 32, nullable = false)
    private AssessmentLevel level;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "min_score", nullable = false)
    @Builder.Default
    private Double minScore = 0.0;

    @Column(name = "max_score", nullable = false)
    @Builder.Default
    private Double maxScore = 100.0;
}
