-- Event tracking table.
-- Stores anonymous and authenticated user actions for analytics/reporting.
-- user_id_hash is SHA-256 of the internal userId — non-reversible.
-- session_id is a browser-generated UUID stored in localStorage.

CREATE TABLE user_action (
    id           BIGSERIAL     PRIMARY KEY,
    session_id   VARCHAR(64),
    user_id_hash VARCHAR(64),
    event_type   VARCHAR(100)  NOT NULL,
    event_data   TEXT,                      -- JSON payload (flexible per event type)
    page         VARCHAR(500),
    referrer     VARCHAR(500),
    user_agent   VARCHAR(500),
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ua_event_type  ON user_action(event_type);
CREATE INDEX idx_ua_created_at  ON user_action(created_at);
CREATE INDEX idx_ua_user_hash   ON user_action(user_id_hash);
CREATE INDEX idx_ua_session_id  ON user_action(session_id);
