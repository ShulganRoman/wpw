-- V11: Replace enum-based user roles with flexible roles + privileges system

-- 1. Create roles table
CREATE TABLE roles (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(50) UNIQUE NOT NULL,
    built_in   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 2. Create role_privileges table
CREATE TABLE role_privileges (
    role_id   BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    privilege VARCHAR(50) NOT NULL,
    PRIMARY KEY (role_id, privilege)
);

-- 3. Drop old users table and enum type, recreate with role_id FK
DROP TABLE IF EXISTS users CASCADE;
DROP TYPE IF EXISTS user_role_enum CASCADE;

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role_id       BIGINT NOT NULL REFERENCES roles(id),
    enabled       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_role_id ON users(role_id);
CREATE INDEX idx_users_enabled ON users(enabled);
