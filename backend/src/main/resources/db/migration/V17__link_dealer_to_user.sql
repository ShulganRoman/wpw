-- V17: Link dealers to users table for login support

ALTER TABLE dealers
    ADD COLUMN IF NOT EXISTS user_id BIGINT REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_dealers_user_id ON dealers(user_id);
