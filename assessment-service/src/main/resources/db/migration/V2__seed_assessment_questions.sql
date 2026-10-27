-- Seed assessment questions and options for MindMap profiling

SET search_path TO assessment_schema;

-- LEVEL 1: Core Personality Profiling (Big Five)
-- Question 1: Extroversion
INSERT INTO questions (id, level, part, question_type, ordinal, question_text, scenario_text, content_warning, is_skippable, time_limit_sec, is_active)
VALUES ('11111111-1111-1111-1111-111111111101', 'LEVEL_1', 'MAIN', 'SINGLE_CHOICE', 1,
        'At social gatherings or professional events, how do you typically regain your energy?',
        NULL, NULL, false, NULL, true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO answer_options (id, question_id, option_label, option_text, trait_key, score_weight, is_safety_flag) VALUES
('21111111-1111-1111-1111-111111111101', '11111111-1111-1111-1111-111111111101', 'A', 'I thrive on interacting with diverse crowds and feel energized by active discussions.', 'EXTROVERSION', 1.0, false),
('21111111-1111-1111-1111-111111111102', '11111111-1111-1111-1111-111111111101', 'B', 'I enjoy small intimate groups and prefer deep conversations over large crowds.', 'EXTROVERSION', 0.5, false),
('21111111-1111-1111-1111-111111111103', '11111111-1111-1111-1111-111111111101', 'C', 'I feel quickly drained by prolonged socializing and need quiet alone time.', 'EXTROVERSION', 0.2, false),
('21111111-1111-1111-1111-111111111104', '11111111-1111-1111-1111-111111111101', 'D', 'I actively avoid social events and feel persistent distress when surrounded by others.', 'NEUROTICISM', 0.8, true)
ON CONFLICT (id) DO NOTHING;

-- Question 2: Conscientiousness
INSERT INTO questions (id, level, part, question_type, ordinal, question_text, scenario_text, content_warning, is_skippable, time_limit_sec, is_active)
VALUES ('11111111-1111-1111-1111-111111111102', 'LEVEL_1', 'MAIN', 'SINGLE_CHOICE', 2,
        'When managing tight deadlines with conflicting priorities, what is your standard approach?',
        NULL, NULL, false, NULL, true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO answer_options (id, question_id, option_label, option_text, trait_key, score_weight, is_safety_flag) VALUES
('21111111-1111-1111-1111-111111111105', '11111111-1111-1111-1111-111111111102', 'A', 'I rigorously schedule milestones and follow structured checklists to ensure delivery.', 'CONSCIENTIOUSNESS', 1.0, false),
('21111111-1111-1111-1111-111111111106', '11111111-1111-1111-1111-111111111102', 'B', 'I adapt flexibly as new information arrives without adhering strictly to plans.', 'OPENNESS', 0.6, false),
('21111111-1111-1111-1111-111111111107', '11111111-1111-1111-1111-111111111102', 'C', 'I tend to procrastinate until urgent pressure forces me into action.', 'CONSCIENTIOUSNESS', 0.2, false),
('21111111-1111-1111-1111-111111111108', '11111111-1111-1111-1111-111111111102', 'D', 'I delegate key aspects and focus primarily on high-level direction.', 'AGREEABLENESS', 0.5, false)
ON CONFLICT (id) DO NOTHING;

-- LEVEL 2: Scenario and Stress Resilience (Part A: MAIN Scenario, Part B: Skippable sensitive questions)
-- Question 3: Level 2 Part A (Scenario)
INSERT INTO questions (id, level, part, question_type, ordinal, question_text, scenario_text, content_warning, is_skippable, time_limit_sec, is_active)
VALUES ('11111111-1111-1111-1111-111111111201', 'LEVEL_2', 'PART_A', 'SCENARIO', 1,
        'How do you respond to the team leader during this production outage incident?',
        'You discover a critical regression in your team software deploy that compromised system availability for 2 hours during peak commercial hours. The team leader asks who authorized the build release.',
        NULL, false, 180, true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO answer_options (id, question_id, option_label, option_text, trait_key, score_weight, is_safety_flag) VALUES
('21111111-1111-1111-1111-111111111201', '11111111-1111-1111-1111-111111111201', 'A', 'Acknowledge personal involvement immediately, present the post-mortem telemetry, and lead the rollback.', 'ETHICAL_ALIGNMENT', 1.0, false),
('21111111-1111-1111-1111-111111111202', '11111111-1111-1111-1111-111111111201', 'B', 'Focus on fixing the system first before investigating or discussing fault attribution.', 'STRESS_RESILIENCE', 0.8, false),
('21111111-1111-1111-1111-111111111203', '11111111-1111-1111-1111-111111111201', 'C', 'Point out gaps in the automated staging validation pipeline that failed to flag the bug.', 'CONSCIENTIOUSNESS', 0.5, false),
('21111111-1111-1111-1111-111111111204', '11111111-1111-1111-1111-111111111201', 'D', 'Feel overwhelmed by blame and want to disconnect completely from the work environment.', 'NEUROTICISM', 0.9, true)
ON CONFLICT (id) DO NOTHING;

-- Question 4: Level 2 Part B (Sensitive / Skippable)
INSERT INTO questions (id, level, part, question_type, ordinal, question_text, scenario_text, content_warning, is_skippable, time_limit_sec, is_active)
VALUES ('11111111-1111-1111-1111-111111111202', 'LEVEL_2', 'PART_B', 'SCENARIO', 2,
        'When recalling past interpersonal trauma or severe career failure, what coping mechanism is most prominent?',
        NULL,
        'This question touches on past emotional distress and vulnerability. You may skip this question without penalty.',
        true, NULL, true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO answer_options (id, question_id, option_label, option_text, trait_key, score_weight, is_safety_flag) VALUES
('21111111-1111-1111-1111-111111111205', '11111111-1111-1111-1111-111111111202', 'A', 'I seek support from mentors or trusted confidants and re-frame the experience constructively.', 'EMPATHIC_RESPONSE', 1.0, false),
('21111111-1111-1111-1111-111111111206', '11111111-1111-1111-1111-111111111202', 'B', 'I channel energy into new projects and compartmentalize the negative emotions.', 'STRESS_RESILIENCE', 0.7, false),
('21111111-1111-1111-1111-111111111207', '11111111-1111-1111-1111-111111111202', 'C', 'I feel feelings of hopelessness that make day-to-day engagement very difficult.', 'NEUROTICISM', 1.0, true)
ON CONFLICT (id) DO NOTHING;

-- LEVEL 3: High-Stakes Crisis Decision Making (Timed Scenario)
-- Question 5: Level 3 (Crisis Decision)
INSERT INTO questions (id, level, part, question_type, ordinal, question_text, scenario_text, content_warning, is_skippable, time_limit_sec, is_active)
VALUES ('11111111-1111-1111-1111-111111111301', 'LEVEL_3', 'MAIN', 'TIMED_SCENARIO', 1,
        'Under acute regulatory scrutiny and a 60-second operational window, which action do you take?',
        'An industrial sensor array indicates a 40% probability of an explosive gas leak in Section 4. Evacuating immediately halts the entire city transit line costing millions and creating public panic, while delaying 15 minutes allows precision verification with drone sensors.',
        'High tension decision simulation under acute temporal constraint.',
        false, 60, true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO answer_options (id, question_id, option_label, option_text, trait_key, score_weight, is_safety_flag) VALUES
('21111111-1111-1111-1111-111111111301', '11111111-1111-1111-1111-111111111301', 'A', 'Execute immediate evacuation of Section 4; human safety strictly overrides financial and schedule penalties.', 'CRISIS_DECISION', 1.0, false),
('21111111-1111-1111-1111-111111111302', '11111111-1111-1111-1111-111111111301', 'B', 'Deploy rapid-response drone sensors while placing emergency crews on high alert without triggering full public transit shutdown.', 'CRISIS_DECISION', 0.9, false),
('21111111-1111-1111-1111-111111111303', '11111111-1111-1111-1111-111111111301', 'C', 'Consult senior command and wait for secondary telemetry verification before taking disruptive action.', 'CRISIS_DECISION', 0.4, false),
('21111111-1111-1111-1111-111111111304', '11111111-1111-1111-1111-111111111301', 'D', 'Freeze in panic, unable to decide under this level of stress.', 'NEUROTICISM', 0.8, true)
ON CONFLICT (id) DO NOTHING;
