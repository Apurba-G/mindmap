-- infra/init-schemas.sql
-- Creates the three schemas on first PostgreSQL startup.

CREATE SCHEMA IF NOT EXISTS auth_schema;
CREATE SCHEMA IF NOT EXISTS assessment_schema;
CREATE SCHEMA IF NOT EXISTS profiling_schema;

-- Grant full access to the application user
GRANT ALL ON SCHEMA auth_schema TO mindmap;
GRANT ALL ON SCHEMA assessment_schema TO mindmap;
GRANT ALL ON SCHEMA profiling_schema TO mindmap;
