-- SQLite baseline (#26): officials, sign-in and the club profile.
--
-- Type conventions for every baseline migration (the PostgreSQL schema they replace is kept in
-- the history of db/migration/postgresql):
--   id                          INTEGER PRIMARY KEY AUTOINCREMENT (ids are never reused)
--   other bigint                BIGINT
--   integer                     INTEGER
--   varchar(n) / text           VARCHAR(n) / TEXT
--   timestamptz                 BIGINT, UTC microseconds since the epoch; columns are named *_at
--                               (plus practice_laps.crossing_time) and read as Instant
--   date                        DATE holding ISO-8601 text (YYYY-MM-DD)
--   boolean                     BOOLEAN holding 0 or 1, checked
--   jsonb                       TEXT holding JSON, checked with json_valid()
--   double precision / numeric  REAL / NUMERIC
--   bytea                       BLOB
-- now() defaults become the current time in microseconds.
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    created_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    updated_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    CONSTRAINT users_email_key UNIQUE (email)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT user_roles_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE refresh_tokens (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    revoked BOOLEAN NOT NULL DEFAULT 0 CHECK (revoked IN (0, 1)),
    CONSTRAINT refresh_tokens_token_hash_key UNIQUE (token_hash),
    CONSTRAINT refresh_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);

CREATE TABLE club_profiles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    website_url VARCHAR(500),
    latitude REAL,
    longitude REAL,
    timezone VARCHAR(100) NOT NULL DEFAULT 'UTC',
    logo BLOB,
    logo_type VARCHAR(10),
    created_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    updated_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    logo_url VARCHAR(500),
    audio_settings TEXT NOT NULL DEFAULT '{"announceFinish": true, "announceLapBeep": true, "announceStagger": true, "announceCountdown": true, "runningOrderDepth": 3, "announceRunningOrder": true}' CHECK (json_valid(audio_settings)),
    default_voice_id VARCHAR(100) NOT NULL DEFAULT 'en_GB-alan-medium',
    decoder_host VARCHAR(255),
    decoder_port INTEGER,
    decoder_protocol VARCHAR(10)
);

CREATE TABLE governing_body_affiliations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code VARCHAR(50) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    membership_required BOOLEAN NOT NULL DEFAULT 0 CHECK (membership_required IN (0, 1)),
    created_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    CONSTRAINT governing_body_affiliations_code_key UNIQUE (code)
);

