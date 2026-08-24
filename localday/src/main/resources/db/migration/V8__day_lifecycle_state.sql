-- V8: day lifecycle state (U10) — singleton row tracking this venue instance's pre-cache /
-- open / close status across the event day.
--
-- Always exactly one row (id = 1), upserted in place rather than versioned per event: a venue
-- laptop runs one event day at a time, and instance_id must stay stable for the lifetime of this
-- database (KTD4 — generation-fencing on the cloud depends on a durable per-instance identity),
-- so this table is created once and reused, never recreated per event.
--
-- All DDL below is plain, transactionally-safe DDL (no CREATE INDEX CONCURRENTLY, no
-- non-transactional ALTER TYPE ... ADD VALUE) per the constraint established in V1.

CREATE TABLE day_lifecycle_state (
    id                    bigint PRIMARY KEY,
    instance_id           varchar(64) NOT NULL,
    cloud_event_id        bigint,
    status                varchar(20) NOT NULL DEFAULT 'NOT_SET_UP',
    generation            bigint,
    -- Plaintext at rest is deliberate, not an oversight: this value is presented to the cloud on
    -- future authenticated calls (U11), not compared against a human-entered secret, so it can't
    -- be hashed. R14's at-rest protection for this column relies on the venue machine's OS-level
    -- full-disk encryption, not application-layer column encryption.
    instance_secret       varchar(500),
    pending_sync_count    int NOT NULL DEFAULT 0,
    split_brain_warning   boolean NOT NULL DEFAULT false,
    last_pre_cached_at    timestamptz,
    created_at            timestamptz NOT NULL DEFAULT now(),
    updated_at            timestamptz NOT NULL DEFAULT now()
);
