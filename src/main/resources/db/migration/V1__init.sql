-- V1: Initial schema
-- This is the authoritative DDL for staging and production.
-- Local dev still uses Hibernate ddl-auto=update (Flyway is disabled locally).

CREATE TABLE app_user (
    id                        BIGSERIAL    PRIMARY KEY,
    email                     VARCHAR(255) NOT NULL UNIQUE,
    user_key                  VARCHAR(255) NOT NULL UNIQUE,
    display_name              VARCHAR(255),
    password_hash             VARCHAR(255),
    google_sub                VARCHAR(255),
    auth_provider             VARCHAR(50)  NOT NULL,
    email_verified            BOOLEAN      NOT NULL DEFAULT FALSE,
    verification_token        VARCHAR(255),
    verification_token_expiry TIMESTAMP,
    created_at                TIMESTAMP    NOT NULL,
    role                      VARCHAR(50)  NOT NULL DEFAULT 'BASIC',
    last_seen_broadcast_id    BIGINT,
    user_type                 VARCHAR(50)  NOT NULL DEFAULT 'BETA'
);

CREATE TABLE card (
    id             BIGSERIAL    PRIMARY KEY,
    user_id        BIGINT       NOT NULL,
    company        VARCHAR(255),
    position       VARCHAR(255),
    stage          VARCHAR(50),
    date           DATE,
    interview_date VARCHAR(255),
    referred_by    VARCHAR(255),
    status         VARCHAR(50),
    details        VARCHAR(255)
);

CREATE TABLE card_history (
    id             BIGSERIAL    PRIMARY KEY,
    card_id        BIGINT,
    sheet_id       BIGINT,
    user_id        BIGINT       NOT NULL,
    changed_at     TIMESTAMPTZ  NOT NULL,
    company        VARCHAR(255),
    position       VARCHAR(255),
    stage          VARCHAR(50),
    date           DATE,
    interview_date VARCHAR(255),
    referred_by    VARCHAR(255),
    status         VARCHAR(50),
    details        VARCHAR(255),
    is_deleted     BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE broadcast_message (
    id         BIGSERIAL    PRIMARY KEY,
    content    VARCHAR(250) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by BIGINT       NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Spring Security remember-me token store (not managed by Hibernate)
CREATE TABLE persistent_logins (
    username  VARCHAR(64) NOT NULL,
    series    VARCHAR(64) PRIMARY KEY,
    token     VARCHAR(64) NOT NULL,
    last_used TIMESTAMP   NOT NULL
);
