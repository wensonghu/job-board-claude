-- Add status and session_token columns to support 7-day guest trial
ALTER TABLE app_user
    ADD COLUMN status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN session_token VARCHAR(64);

CREATE UNIQUE INDEX idx_app_user_session_token
    ON app_user(session_token)
    WHERE session_token IS NOT NULL;

-- All pre-existing users are already registered
UPDATE app_user SET status = 'REGISTERED';
