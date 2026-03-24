-- V12: User survey — shown once on blank board to understand why users aren't adding cards
CREATE TABLE user_survey (
    id               BIGSERIAL PRIMARY KEY,
    job_search_stage VARCHAR(60),
    search_duration  VARCHAR(30),
    support_need     VARCHAR(40),
    find_helpful     VARCHAR(3),
    helpful_reason   TEXT,
    email            VARCHAR(254),
    created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_survey_created_at ON user_survey(created_at DESC);
