-- Spring Security persistent_logins table for 90-day remember-me tokens.
-- Hibernate ddl-auto=update does not manage non-JPA tables, so we create it here.
CREATE TABLE IF NOT EXISTS persistent_logins (
    username  VARCHAR(64) NOT NULL,
    series    VARCHAR(64) PRIMARY KEY,
    token     VARCHAR(64) NOT NULL,
    last_used TIMESTAMP   NOT NULL
);
