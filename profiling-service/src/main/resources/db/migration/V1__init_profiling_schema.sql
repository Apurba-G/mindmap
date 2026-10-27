-- V1__init_profiling_schema.sql
-- MindMap Profiling Service Initial Schema

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. User Profiles
CREATE TABLE IF NOT EXISTS user_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE,
    highest_level VARCHAR(50),
    overall_confidence DOUBLE PRECISION DEFAULT 0.0,
    is_complete BOOLEAN NOT NULL DEFAULT FALSE,
    encrypted_summary BYTEA,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_profiles_user_id ON user_profiles (user_id);

-- 2. Profile Scores
CREATE TABLE IF NOT EXISTS profile_scores (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    profile_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    level VARCHAR(50) NOT NULL,
    trait_key VARCHAR(100) NOT NULL,
    raw_score DOUBLE PRECISION NOT NULL,
    normalized DOUBLE PRECISION NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_profile_score_level_trait UNIQUE (profile_id, level, trait_key)
);

CREATE INDEX IF NOT EXISTS idx_profile_scores_profile_level ON profile_scores (profile_id, level);

-- 3. Temperament Profiles
CREATE TABLE IF NOT EXISTS temperament_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    profile_id UUID NOT NULL UNIQUE REFERENCES user_profiles(id) ON DELETE CASCADE,
    strong_calm_pct DOUBLE PRECISION NOT NULL,
    moderate_temper_pct DOUBLE PRECISION NOT NULL,
    low_control_angry_pct DOUBLE PRECISION NOT NULL,
    low_control_confused_pct DOUBLE PRECISION NOT NULL,
    primary_type VARCHAR(50) NOT NULL,
    explanation_text TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_temperament_profile_id ON temperament_profiles (profile_id);

-- 4. Scoring Rules
CREATE TABLE IF NOT EXISTS scoring_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    level VARCHAR(50) NOT NULL,
    trait_key VARCHAR(100) NOT NULL,
    rule_name VARCHAR(200) NOT NULL,
    rule_type VARCHAR(50) NOT NULL,
    parameters JSONB NOT NULL DEFAULT '{}'::jsonb,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_scoring_rules_level_active ON scoring_rules (level, is_active);
CREATE INDEX IF NOT EXISTS idx_scoring_rules_trait ON scoring_rules (level, trait_key, is_active);

-- 5. Profile Access Audit Log
CREATE TABLE IF NOT EXISTS profile_access_audits (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    admin_user_id UUID NOT NULL,
    admin_handle VARCHAR(100) NOT NULL,
    target_user_id UUID NOT NULL,
    access_type VARCHAR(100) NOT NULL,
    ip_address VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_admin ON profile_access_audits (admin_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_target ON profile_access_audits (target_user_id, created_at DESC);

-- 6. User Summaries
CREATE TABLE IF NOT EXISTS user_summaries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    profile_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    level VARCHAR(50) NOT NULL,
    summary_text TEXT NOT NULL,
    visibility VARCHAR(50) NOT NULL DEFAULT 'SHORT',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_summary_profile_level UNIQUE (profile_id, level)
);

CREATE INDEX IF NOT EXISTS idx_user_summaries_profile_level ON user_summaries (profile_id, level);

-- Seed Initial Scoring Rules
INSERT INTO scoring_rules (id, level, trait_key, rule_name, rule_type, parameters, is_active, version)
VALUES
    -- Level 1 (Big Five)
    (uuid_generate_v4(), 'LEVEL_1', 'openness', 'Openness to Experience Rule', 'WEIGHT',
     '{"maxScore": 50.0, "minScore": 0.0, "minResponseMs": 1500, "fastAnswerPenalty": 5.0, "contradictionDelta": 10.0}'::jsonb, TRUE, 1),
    (uuid_generate_v4(), 'LEVEL_1', 'conscientiousness', 'Conscientiousness Rule', 'WEIGHT',
     '{"maxScore": 50.0, "minScore": 0.0, "minResponseMs": 1500, "fastAnswerPenalty": 5.0, "contradictionDelta": 10.0}'::jsonb, TRUE, 1),
    (uuid_generate_v4(), 'LEVEL_1', 'extraversion', 'Extraversion Rule', 'WEIGHT',
     '{"maxScore": 50.0, "minScore": 0.0, "minResponseMs": 1500, "fastAnswerPenalty": 5.0, "contradictionDelta": 10.0}'::jsonb, TRUE, 1),
    (uuid_generate_v4(), 'LEVEL_1', 'agreeableness', 'Agreeableness Rule', 'WEIGHT',
     '{"maxScore": 50.0, "minScore": 0.0, "minResponseMs": 1500, "fastAnswerPenalty": 5.0, "contradictionDelta": 10.0}'::jsonb, TRUE, 1),
    (uuid_generate_v4(), 'LEVEL_1', 'emotional_stability', 'Emotional Stability Rule', 'WEIGHT',
     '{"maxScore": 50.0, "minScore": 0.0, "minResponseMs": 1500, "fastAnswerPenalty": 5.0, "contradictionDelta": 10.0}'::jsonb, TRUE, 1),

    -- Level 2 (Ambiguity + Dark Theme + Moral Flexibility + Consistency)
    (uuid_generate_v4(), 'LEVEL_2', 'ambiguity_tolerance', 'Ambiguity Tolerance Rule', 'WEIGHT',
     '{"maxScore": 40.0, "minScore": 0.0, "minResponseMs": 1200, "fastAnswerPenalty": 4.0}'::jsonb, TRUE, 1),
    (uuid_generate_v4(), 'LEVEL_2', 'dark_theme_tolerance', 'Dark Theme Tolerance Rule', 'WEIGHT',
     '{"maxScore": 40.0, "minScore": 0.0, "minResponseMs": 1200, "fastAnswerPenalty": 4.0}'::jsonb, TRUE, 1),
    (uuid_generate_v4(), 'LEVEL_2', 'moral_flexibility', 'Moral Flexibility Rule', 'WEIGHT',
     '{"maxScore": 40.0, "minScore": 0.0, "minResponseMs": 1200, "fastAnswerPenalty": 4.0}'::jsonb, TRUE, 1),
    (uuid_generate_v4(), 'LEVEL_2', 'consistency', 'Response Consistency Rule', 'FORMULA',
     '{"maxScore": 100.0, "minScore": 0.0, "contradictionPenalty": 15.0}'::jsonb, TRUE, 1),

    -- Level 3 (Temperament Axes)
    (uuid_generate_v4(), 'LEVEL_3', 'emotional_control', 'Emotional Control Axis Rule', 'WEIGHT',
     '{"maxScore": 50.0, "minScore": 0.0, "minResponseMs": 1000}'::jsonb, TRUE, 1),
    (uuid_generate_v4(), 'LEVEL_3', 'impulsiveness', 'Impulsiveness Inversion Axis Rule', 'WEIGHT',
     '{"maxScore": 50.0, "minScore": 0.0, "minResponseMs": 1000, "invert": true}'::jsonb, TRUE, 1),
    (uuid_generate_v4(), 'LEVEL_3', 'clarity_of_thinking', 'Clarity of Thinking Axis Rule', 'WEIGHT',
     '{"maxScore": 50.0, "minScore": 0.0, "minResponseMs": 1000}'::jsonb, TRUE, 1)
ON CONFLICT DO NOTHING;
