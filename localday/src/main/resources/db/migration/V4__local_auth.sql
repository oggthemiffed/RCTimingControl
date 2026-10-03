-- V4: Local authentication — day-scoped official credentials and sessions (U6).
--
-- Distinct from the cloud's stateless-JWT session model (KTD5): a LocalCredential is a
-- per-official, per-event-day passcode (BCrypt-hashed, low-entropy human-enterable secret) and
-- a LocalSession is a plain high-entropy random token issued on successful login. Minting of
-- local_credentials rows from the cloud happens in a later unit (U10/U13) — this migration only
-- creates the tables the local validate/session/lockout/recovery machinery reads and writes.
--
-- All DDL below is plain, transactionally-safe DDL (no CREATE INDEX CONCURRENTLY, no
-- non-transactional ALTER TYPE ... ADD VALUE) per the constraint established in V1.

CREATE TABLE local_credentials (
    id                    bigserial PRIMARY KEY,
    cloud_user_id         bigint,
    official_name         varchar(200) NOT NULL,
    secret_hash           varchar(200) NOT NULL,
    recovery              boolean NOT NULL DEFAULT false,
    failed_attempt_count  int NOT NULL DEFAULT 0,
    locked_until          timestamptz,
    created_at            timestamptz NOT NULL DEFAULT now()
);

-- Session tokens are looked up by exact equality on every authenticated request; the UNIQUE
-- constraint below gives Postgres an automatic index on session_token, so no separate
-- CREATE INDEX is needed for that column.
CREATE TABLE local_sessions (
    id                  bigserial PRIMARY KEY,
    credential_id       bigint NOT NULL REFERENCES local_credentials(id),
    official_name       varchar(200) NOT NULL,
    session_token       varchar(200) NOT NULL,
    issued_at           timestamptz NOT NULL,
    UNIQUE(session_token)
);

CREATE INDEX idx_local_sessions_credential_id ON local_sessions(credential_id);
