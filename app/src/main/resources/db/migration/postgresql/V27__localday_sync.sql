-- V27: Local Race Day Program (:localday) cloud-side sync support
-- Adds the day-open/close lock, the sync-generation counter for a future snapshot-ingest
-- fencing unit, day-scoped official credentials, and per-instance sync-channel secrets.

CREATE TABLE event_offline_locks (
    event_id      bigint PRIMARY KEY REFERENCES events(id),
    locked_at     timestamptz NOT NULL,
    unlocked_at   timestamptz
);

COMMENT ON TABLE event_offline_locks IS
    'Static open/closed lock for an event handed to the Local Race Day Program (KD4/R2) -- set at day-open, cleared at day-close once :localday confirms its own sync is complete. Not a live-monitored lock -- no heartbeat, per KD4.';

CREATE TABLE event_sync_generations (
    event_id    bigint PRIMARY KEY REFERENCES events(id),
    generation  bigint NOT NULL DEFAULT 0
);

COMMENT ON TABLE event_sync_generations IS
    'Stored generation-per-event for a future unit''s snapshot-ingest fencing (KTD4). Each pre-cache call atomically increments this and hands the new value to the calling :localday instance as its claimed generation for the session. The compare-and-reject logic on snapshot ingest itself is a future unit -- this table only tracks the counter.';

CREATE TABLE localday_credentials (
    id            bigserial PRIMARY KEY,
    event_id      bigint NOT NULL REFERENCES events(id),
    user_id       bigint NOT NULL REFERENCES users(id),
    secret_hash   varchar(255) NOT NULL,
    issued_at     timestamptz NOT NULL
);

CREATE INDEX idx_localday_credentials_event_id ON localday_credentials(event_id);
CREATE UNIQUE INDEX idx_localday_credentials_event_user ON localday_credentials(event_id, user_id);

COMMENT ON TABLE localday_credentials IS
    'Day-scoped local login credentials minted at pre-cache time (KTD5) for officials working an event with the Local Race Day Program. secret_hash is a BCrypt hash of a randomly generated 6-digit PIN -- the plaintext PIN is returned once in the pre-cache response and never stored. Distinct from users.password_hash (the cloud login credential) per KTD5. Re-calling pre-cache for the same (event, user) replaces the row -- the previous PIN stops being mintable/re-showable, which is why the unique index exists.';

CREATE TABLE localday_instance_secrets (
    id              bigserial PRIMARY KEY,
    event_id        bigint NOT NULL REFERENCES events(id),
    instance_id     varchar(64) NOT NULL,
    secret_hash     varchar(255) NOT NULL,
    issued_at       timestamptz NOT NULL,
    invalidated_at  timestamptz
);

CREATE UNIQUE INDEX idx_localday_instance_secrets_event_instance ON localday_instance_secrets(event_id, instance_id);

COMMENT ON TABLE localday_instance_secrets IS
    'Per-day-instance sync-channel secret (KTD9), minted alongside officials'' credentials at pre-cache time, keyed by the calling :localday instance''s own self-generated instance_id (a stable UUID string the local install generates once and keeps for its lifetime). Machine-to-machine identity distinct from officials'' local session auth (KTD5) -- authenticates a future snapshot-push channel. invalidated_at is set by a future device-loss declaration (R16), not used yet.';
