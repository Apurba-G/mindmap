-- Create Schema
CREATE SCHEMA IF NOT EXISTS assessment_schema;

SET search_path TO assessment_schema;

-- Trait Definitions Table
CREATE TABLE IF NOT EXISTS trait_definitions (
    key VARCHAR(64) PRIMARY KEY,
    display_name VARCHAR(128) NOT NULL,
    level VARCHAR(32) NOT NULL,
    description TEXT,
    min_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    max_score DOUBLE PRECISION NOT NULL DEFAULT 100.0
);

-- Questions Table
CREATE TABLE IF NOT EXISTS questions (
    id UUID PRIMARY KEY,
    level VARCHAR(32) NOT NULL,
    part VARCHAR(32) NOT NULL,
    question_type VARCHAR(32) NOT NULL,
    ordinal INTEGER NOT NULL,
    question_text TEXT NOT NULL,
    scenario_text TEXT,
    content_warning TEXT,
    is_skippable BOOLEAN NOT NULL DEFAULT FALSE,
    time_limit_sec INTEGER,
    follow_up_of UUID REFERENCES questions(id) ON DELETE SET NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Answer Options Table
CREATE TABLE IF NOT EXISTS answer_options (
    id UUID PRIMARY KEY,
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    option_label VARCHAR(8) NOT NULL,
    option_text TEXT NOT NULL,
    trait_key VARCHAR(64) REFERENCES trait_definitions(key) ON DELETE SET NULL,
    score_weight DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    is_safety_flag BOOLEAN NOT NULL DEFAULT FALSE
);

-- Assessment Sessions Table
CREATE TABLE IF NOT EXISTS assessment_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    level VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    total_time_sec INTEGER
);

-- User Answers Table
CREATE TABLE IF NOT EXISTS user_answers (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES assessment_sessions(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    selected_option_id UUID REFERENCES answer_options(id) ON DELETE SET NULL,
    was_skipped BOOLEAN NOT NULL DEFAULT FALSE,
    response_time_ms BIGINT,
    answered_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    encrypted_raw BYTEA,
    CONSTRAINT uq_session_question UNIQUE (session_id, question_id)
);

-- Content Warning Acknowledgments Table
CREATE TABLE IF NOT EXISTS content_warning_acks (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    level VARCHAR(32) NOT NULL,
    part VARCHAR(32) NOT NULL,
    accepted BOOLEAN NOT NULL DEFAULT FALSE,
    acked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_level_part UNIQUE (user_id, level, part)
);

-- Safety Flags Table
CREATE TABLE IF NOT EXISTS safety_flags (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    session_id UUID NOT NULL,
    question_id UUID NOT NULL,
    flag_type VARCHAR(64) NOT NULL,
    details TEXT,
    reviewed BOOLEAN NOT NULL DEFAULT FALSE,
    reviewed_by VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_questions_level_part_ordinal ON questions (level, part, is_active, ordinal);
CREATE INDEX IF NOT EXISTS idx_answer_options_question ON answer_options (question_id);
CREATE INDEX IF NOT EXISTS idx_assessment_sessions_user_status ON assessment_sessions (user_id, status);
CREATE INDEX IF NOT EXISTS idx_assessment_sessions_user_level ON assessment_sessions (user_id, level);
CREATE INDEX IF NOT EXISTS idx_user_answers_session ON user_answers (session_id);
CREATE INDEX IF NOT EXISTS idx_safety_flags_reviewed_created ON safety_flags (reviewed, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_content_warning_ack ON content_warning_acks (user_id, level, part);

-- Initial Trait Definitions Seed Data
INSERT INTO trait_definitions (key, display_name, level, description, min_score, max_score) VALUES
('EXTROVERSION', 'Extroversion', 'LEVEL_1', 'Tendency to seek stimulation in the company of others', 0.0, 100.0),
('AGREEABLENESS', 'Agreeableness', 'LEVEL_1', 'Tendency toward cooperation and social harmony', 0.0, 100.0),
('CONSCIENTIOUSNESS', 'Conscientiousness', 'LEVEL_1', 'Tendency to exercise self-discipline and act dutifully', 0.0, 100.0),
('NEUROTICISM', 'Emotional Stability / Neuroticism', 'LEVEL_1', 'Tendency toward psychological distress and emotional vulnerability', 0.0, 100.0),
('OPENNESS', 'Openness to Experience', 'LEVEL_1', 'Appreciation for art, emotion, adventure, and unusual ideas', 0.0, 100.0),
('STRESS_RESILIENCE', 'Stress Resilience', 'LEVEL_2', 'Ability to mentally or emotionally cope with a crisis', 0.0, 100.0),
('ETHICAL_ALIGNMENT', 'Ethical Alignment', 'LEVEL_2', 'Moral reasoning and adherence to ethical principles under pressure', 0.0, 100.0),
('EMPATHIC_RESPONSE', 'Empathic Response', 'LEVEL_2', 'Capacity to understand or feel what another person is experiencing', 0.0, 100.0),
('CRISIS_DECISION', 'Crisis Decision Making', 'LEVEL_3', 'Speed and composure under severe, time-critical dilemmas', 0.0, 100.0)
ON CONFLICT (key) DO NOTHING;
