package com.mindmap.assessment.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Question entity representing a psychological assessment item.
 */
@Entity
@Table(name = "questions", schema = "assessment_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", length = 32, nullable = false)
    private AssessmentLevel level;

    @Enumerated(EnumType.STRING)
    @Column(name = "part", length = 32, nullable = false)
    private AssessmentPart part;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", length = 32, nullable = false)
    private QuestionType questionType;

    @Column(name = "ordinal", nullable = false)
    private Integer ordinal;

    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Column(name = "scenario_text", columnDefinition = "TEXT")
    private String scenarioText;

    @Column(name = "content_warning", columnDefinition = "TEXT")
    private String contentWarning;

    @Column(name = "is_skippable", nullable = false)
    @Builder.Default
    private boolean isSkippable = false;

    @Column(name = "time_limit_sec")
    private Integer timeLimitSec;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follow_up_of")
    private Question followUpOf;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("optionLabel ASC")
    @Builder.Default
    private List<AnswerOption> options = new ArrayList<>();

    public void addOption(AnswerOption option) {
        options.add(option);
        option.setQuestion(this);
    }

    public void removeOption(AnswerOption option) {
        options.remove(option);
        option.setQuestion(null);
    }
}
