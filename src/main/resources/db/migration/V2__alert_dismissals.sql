-- V2: Persist alert dismissals so interview reminders survive page refreshes
CREATE TABLE alert_dismissal (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    alert_key    VARCHAR(100) NOT NULL,
    dismissed_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, alert_key)
);
