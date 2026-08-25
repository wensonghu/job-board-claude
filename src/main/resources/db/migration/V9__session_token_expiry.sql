-- Add expiry timestamp for persistent session tokens (90-day browser auth for all user types)
ALTER TABLE app_user
    ADD COLUMN session_token_expires_at TIMESTAMP;
