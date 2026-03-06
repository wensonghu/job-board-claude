-- Spring Security persistent_logins table for 90-day remember-me tokens.
-- Hibernate ddl-auto=update does not manage non-JPA tables, so we create it here.
CREATE TABLE IF NOT EXISTS persistent_logins (
    username  VARCHAR(64) NOT NULL,
    series    VARCHAR(64) PRIMARY KEY,
    token     VARCHAR(64) NOT NULL,
    last_used TIMESTAMP   NOT NULL
);

-- user_action indexes for local dev (table itself is created by Hibernate ddl-auto=update).
-- On staging these indexes are created by V7 Flyway migration.
CREATE INDEX IF NOT EXISTS idx_ua_event_type ON user_action(event_type);
CREATE INDEX IF NOT EXISTS idx_ua_created_at ON user_action(created_at);
CREATE INDEX IF NOT EXISTS idx_ua_user_hash  ON user_action(user_id_hash);
CREATE INDEX IF NOT EXISTS idx_ua_session_id ON user_action(session_id);
