-- V9: snapshot_queue (U11) — durable cursor tracking this venue instance's outbound periodic
-- snapshot sync to the cloud (KTD4, KTD8). Singleton row (id = 1), same convention as
-- day_lifecycle_state (V8).
--
-- last_synced_lap_id is the high-water mark of the last lap_passings.id successfully included
-- in a snapshot the cloud accepted; laps with id > this value are "pending" and included
-- (incrementally, per KTD8) in the next push attempt.
--
-- pending_snapshot_id / pending_lap_id_ceiling together identify the in-flight, not-yet-
-- acknowledged snapshot attempt (if any): the same snapshotId is reused across retries as long
-- as the lap range being sent hasn't grown, so a retried upload after a flaky connection can't
-- double-apply on the cloud side (KTD4's snapshotId idempotency check). A new lap arriving
-- during an outage widens the range and mints a fresh snapshotId for the next attempt, since
-- that is genuinely new content, not a retry.
--
-- All DDL below is plain, transactionally-safe DDL (no CREATE INDEX CONCURRENTLY, no
-- non-transactional ALTER TYPE ... ADD VALUE) per the constraint established in V1.

CREATE TABLE snapshot_queue (
    id                       bigint PRIMARY KEY,
    last_synced_lap_id       bigint,
    pending_snapshot_id      varchar(64),
    pending_lap_id_ceiling   bigint,
    last_push_attempt_at     timestamptz,
    last_push_success_at     timestamptz,
    updated_at               timestamptz NOT NULL DEFAULT now()
);
