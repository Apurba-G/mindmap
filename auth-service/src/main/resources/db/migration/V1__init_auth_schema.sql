-- MindMap Platform: Auth Schema Initialization
CREATE SCHEMA IF NOT EXISTS auth_schema;

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Users table
CREATE TABLE IF NOT EXISTS auth_schema.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(50) NOT NULL UNIQUE,
    user_id_lower VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    recovery_hash VARCHAR(255) NOT NULL,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_admin_seeded BOOLEAN NOT NULL DEFAULT FALSE,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- User Roles junction table
CREATE TABLE IF NOT EXISTS auth_schema.user_roles (
    user_id UUID NOT NULL REFERENCES auth_schema.users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, role)
);

-- Reserved Usernames table
CREATE TABLE IF NOT EXISTS auth_schema.reserved_usernames (
    id BIGSERIAL PRIMARY KEY,
    word VARCHAR(50) NOT NULL UNIQUE,
    reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Login Attempts table
CREATE TABLE IF NOT EXISTS auth_schema.login_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(50),
    ip_address VARCHAR(45) NOT NULL,
    successful BOOLEAN NOT NULL,
    failure_reason VARCHAR(100),
    attempted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Recovery Attempts table
CREATE TABLE IF NOT EXISTS auth_schema.recovery_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(50) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    successful BOOLEAN NOT NULL,
    failure_reason VARCHAR(100),
    attempted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Audit Log table
CREATE TABLE IF NOT EXISTS auth_schema.audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID,
    actor_user_id VARCHAR(50),
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id VARCHAR(100),
    metadata TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance & query optimization
CREATE INDEX IF NOT EXISTS idx_users_user_id_lower ON auth_schema.users(user_id_lower);
CREATE INDEX IF NOT EXISTS idx_users_is_active ON auth_schema.users(is_active);
CREATE INDEX IF NOT EXISTS idx_reserved_usernames_word ON auth_schema.reserved_usernames(word);
CREATE INDEX IF NOT EXISTS idx_login_attempts_user_id_time ON auth_schema.login_attempts(user_id, attempted_at DESC);
CREATE INDEX IF NOT EXISTS idx_login_attempts_ip_time ON auth_schema.login_attempts(ip_address, attempted_at DESC);
CREATE INDEX IF NOT EXISTS idx_recovery_attempts_user_id_time ON auth_schema.recovery_attempts(user_id, attempted_at DESC);
CREATE INDEX IF NOT EXISTS idx_recovery_attempts_ip_time ON auth_schema.recovery_attempts(ip_address, attempted_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_actor ON auth_schema.audit_log(actor_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_resource ON auth_schema.audit_log(resource_type, resource_id);

-- Initial seed of reserved usernames
INSERT INTO auth_schema.reserved_usernames (word, reason) VALUES
    ('admin', 'Reserved system administrator identifier'),
    ('administrator', 'Reserved system administrator identifier'),
    ('root', 'Reserved root system identifier'),
    ('system', 'Reserved system core identifier'),
    ('support', 'Reserved customer support identifier'),
    ('help', 'Reserved help desk identifier'),
    ('api', 'Reserved API gateway route identifier'),
    ('auth', 'Reserved authentication service identifier'),
    ('test', 'Reserved test identifier'),
    ('guest', 'Reserved guest identifier'),
    ('mindmap', 'Reserved platform brand identifier'),
    ('official', 'Reserved official brand identifier'),
    ('moderator', 'Reserved moderation role identifier'),
    ('mod', 'Reserved moderation role identifier'),
    ('null', 'Reserved programming keyword'),
    ('undefined', 'Reserved programming keyword'),
    ('security', 'Reserved security team identifier'),
    ('superuser', 'Reserved superuser identifier'),
    ('anonymous', 'Reserved anonymous identifier'),
    ('operator', 'Reserved operations identifier'),
    ('dev', 'Reserved developer identifier'),
    ('developer', 'Reserved developer identifier'),
    ('staff', 'Reserved staff identifier'),
    ('internal', 'Reserved internal identifier')
ON CONFLICT (word) DO NOTHING;
