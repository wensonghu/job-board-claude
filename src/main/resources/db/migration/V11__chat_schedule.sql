-- Chat availability schedule (one row per ISO day: 1=Mon ... 7=Sun)
CREATE TABLE chat_schedule (
    day_of_week SMALLINT PRIMARY KEY,
    enabled     BOOLEAN  NOT NULL DEFAULT TRUE,
    open_time   TIME     NOT NULL DEFAULT '09:00',
    close_time  TIME     NOT NULL DEFAULT '17:00'
);

INSERT INTO chat_schedule (day_of_week, enabled, open_time, close_time) VALUES
    (1, true,  '09:00', '17:00'),
    (2, true,  '09:00', '17:00'),
    (3, true,  '09:00', '17:00'),
    (4, true,  '09:00', '17:00'),
    (5, true,  '09:00', '17:00'),
    (6, false, '09:00', '17:00'),
    (7, false, '09:00', '17:00');

-- Generic key/value config (used for chat_timezone, etc.)
CREATE TABLE app_config (
    setting_key   VARCHAR(64) PRIMARY KEY,
    setting_value TEXT        NOT NULL
);

INSERT INTO app_config (setting_key, setting_value) VALUES
    ('chat_timezone', 'America/New_York');
