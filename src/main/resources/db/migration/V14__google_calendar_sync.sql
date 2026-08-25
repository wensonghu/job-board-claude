CREATE TABLE google_calendar_token (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL UNIQUE REFERENCES app_user(id),
    google_email    VARCHAR(255),
    access_token    TEXT NOT NULL,
    refresh_token   TEXT NOT NULL,
    token_expiry    TIMESTAMPTZ NOT NULL,
    scope_granted   VARCHAR(255) NOT NULL,
    sync_token      TEXT,
    last_synced_at  TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE card ADD COLUMN google_event_id VARCHAR(255);
CREATE INDEX idx_card_google_event_id ON card(google_event_id);
