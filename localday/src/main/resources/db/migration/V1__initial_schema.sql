-- V1: Initial schema for the offline race-day local store.
--
-- This is an independent migration history — it does not share numbering, tables, or a
-- flyway_schema_history row with app/'s migrations. The localday database is a separate,
-- purely local PostgreSQL instance (embedded, per venue laptop) that caches a snapshot of
-- cloud data for the race day and durably records lap passings while offline.
--
-- All DDL below is plain, transactionally-safe DDL (no CREATE INDEX CONCURRENTLY, no
-- non-transactional ALTER TYPE ... ADD VALUE) so a crash mid-migration always rolls back
-- cleanly under Flyway's per-migration transaction wrapping.

-- Cached snapshot of racer/entry data pulled from the cloud before the event goes offline.
-- Minimal denormalised fields — this is a read-mostly local cache, not a system of record.
CREATE TABLE cached_entries (
    id                  bigserial PRIMARY KEY,
    cloud_entry_id      bigint NOT NULL,
    transponder_number  varchar(20) NOT NULL,
    racer_name          varchar(200) NOT NULL,
    car_name            varchar(200),
    class_name          varchar(100),
    cloud_event_class_id bigint,
    status              varchar(20) NOT NULL DEFAULT 'ACTIVE',
    synced_at           timestamptz NOT NULL DEFAULT now(),
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),
    UNIQUE (cloud_entry_id)
);

CREATE INDEX idx_cached_entries_transponder_number ON cached_entries(transponder_number);

-- Cached snapshot of the event day's race schedule (rounds/races) pulled from the cloud.
CREATE TABLE cached_schedule (
    id                  bigserial PRIMARY KEY,
    cloud_race_id       bigint NOT NULL,
    round_number        int NOT NULL,
    heat_number         int NOT NULL,
    sequence            int NOT NULL,
    class_name          varchar(100),
    final_letter        varchar(5),
    scheduled_start_at  timestamptz,
    status              varchar(20) NOT NULL DEFAULT 'PENDING',
    synced_at           timestamptz NOT NULL DEFAULT now(),
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),
    UNIQUE (cloud_race_id)
);

CREATE INDEX idx_cached_schedule_sequence ON cached_schedule(sequence);

-- Cached snapshot of the race format config assigned to a race/class, as JSONB — mirrors the
-- app/ module's snapshot-at-assignment approach (FORMAT-06): template edits made in the cloud
-- after the snapshot was cached do not retroactively affect a race already running locally.
CREATE TABLE cached_format_configs (
    id                  bigserial PRIMARY KEY,
    cloud_format_id     bigint NOT NULL,
    name                varchar(200) NOT NULL,
    config              jsonb NOT NULL,
    synced_at           timestamptz NOT NULL DEFAULT now(),
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),
    UNIQUE (cloud_format_id)
);

-- Durable local lap-timing storage. This is the system of record while offline — every raw
-- passing received from the decoder (via TCP Receiver / decoder-protocol) is written here
-- before anything is broadcast or aggregated, so a crash never loses a passing that was
-- already acknowledged to the decoder hardware.
CREATE TABLE lap_passings (
    id                  bigserial PRIMARY KEY,
    cached_schedule_id  bigint REFERENCES cached_schedule(id),
    transponder_number  varchar(20) NOT NULL,
    passing_at           timestamptz NOT NULL,
    lap_time_ms         bigint,
    lap_number          int,
    raw_decoder_line    text,
    created_at          timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_lap_passings_schedule_id ON lap_passings(cached_schedule_id);
CREATE INDEX idx_lap_passings_transponder_number ON lap_passings(transponder_number);
CREATE INDEX idx_lap_passings_passing_at ON lap_passings(passing_at);
