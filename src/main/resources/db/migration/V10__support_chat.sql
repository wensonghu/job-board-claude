-- Live support chat between users (PENDING and REGISTERED) and admin
CREATE TABLE support_chat_session (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES app_user(id),
    status     VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    closed_at  TIMESTAMP
);

CREATE TABLE support_chat_message (
    id          BIGSERIAL PRIMARY KEY,
    session_id  BIGINT NOT NULL REFERENCES support_chat_session(id),
    sender_type VARCHAR(10) NOT NULL,  -- 'USER' or 'ADMIN' or 'SYSTEM'
    message     TEXT NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_session_user   ON support_chat_session(user_id);
CREATE INDEX idx_chat_session_status ON support_chat_session(status);
CREATE INDEX idx_chat_message_sess   ON support_chat_message(session_id);
