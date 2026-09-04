ALTER TABLE users
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN avatar_url VARCHAR(512),
    ADD COLUMN verification_token VARCHAR(255),
    ADD COLUMN verification_token_expires_at TIMESTAMP,
    ADD COLUMN password_reset_token VARCHAR(255),
    ADD COLUMN password_reset_token_expires_at TIMESTAMP;

CREATE INDEX idx_users_verification_token ON users (verification_token);
CREATE INDEX idx_users_password_reset_token ON users (password_reset_token);
