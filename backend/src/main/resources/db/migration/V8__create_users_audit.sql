CREATE TYPE user_role_enum AS ENUM ('admin', 'editor', 'viewer');

CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(100) UNIQUE NOT NULL,
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(60) NOT NULL,
    role          user_role_enum NOT NULL DEFAULT 'viewer',
    is_active     BOOLEAN NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ DEFAULT NOW(),
    last_login_at TIMESTAMPTZ
);

CREATE TABLE content_audit_log (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL,
    entity_id   UUID,
    action      VARCHAR(30) NOT NULL,
    changed_by  VARCHAR(200),
    changed_at  TIMESTAMPTZ DEFAULT NOW(),
    payload     JSONB
);

CREATE INDEX idx_audit_entity     ON content_audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_changed_at ON content_audit_log (changed_at DESC);
