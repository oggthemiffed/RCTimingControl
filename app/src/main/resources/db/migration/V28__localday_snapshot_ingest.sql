-- V28: Local Race Day Program (:localday) snapshot ingest, device-loss declaration, and public
-- visibility support (U14).

-- R17: a device-loss declaration permanently flags an event day's data as having a known,
-- unrecoverable gap. Colocated on event_offline_locks rather than a new table -- it is set at
-- the exact same moment DeviceLossController unlocks the day, and this table already carries
-- this event's other offline-day operational state (V27).
ALTER TABLE event_offline_locks ADD COLUMN incomplete_data boolean NOT NULL DEFAULT false;
ALTER TABLE event_offline_locks ADD COLUMN incomplete_data_declared_at timestamptz;

COMMENT ON COLUMN event_offline_locks.incomplete_data IS
    'R17: set once by a device-loss declaration (DeviceLossController) and never cleared -- an unrecoverable local data gap is permanent, unlike R15''s transient outage indicator.';

-- KTD4 idempotency: a unique (event_id, snapshot_id) row per accepted snapshot. A repeated
-- snapshotId short-circuits to the original accepted outcome without reprocessing -- enforced by
-- the unique index below, not just application logic (a race between two identical retries is
-- resolved by whichever insert wins; the loser's DataIntegrityViolationException is caught and
-- treated as an idempotent replay, same "try insert, catch, re-read" upsert idiom already used
-- by DayLifecycleService.upsertLock / PreCacheService's upsert helpers).
CREATE TABLE localday_snapshots (
    id            bigserial PRIMARY KEY,
    event_id      bigint NOT NULL REFERENCES events(id),
    instance_id   varchar(64) NOT NULL,
    snapshot_id   varchar(64) NOT NULL,
    generation    bigint NOT NULL,
    received_at   timestamptz NOT NULL
);

CREATE UNIQUE INDEX idx_localday_snapshots_event_snapshot ON localday_snapshots(event_id, snapshot_id);
CREATE INDEX idx_localday_snapshots_event_id ON localday_snapshots(event_id);

COMMENT ON TABLE localday_snapshots IS
    'One row per accepted snapshot push (KTD4) -- exists solely to answer "have we already processed this exact snapshotId for this event" idempotently. Rejected (superseded) attempts are not recorded here: their outcome is deterministic and stable for a given generation, so there is nothing to remember.';

-- R11/R15: the latest accepted snapshot's payload, stored verbatim as JSONB (the cloud does not
-- parse or validate its internal shape in this unit -- it is captured and republished for the
-- public page, not fed into the cloud's own race-control/standings engine, which is out of this
-- unit's scope). One row per event, upserted on every accepted push.
CREATE TABLE event_snapshot_state (
    event_id         bigint PRIMARY KEY REFERENCES events(id),
    last_synced_at   timestamptz NOT NULL,
    payload          jsonb NOT NULL
);

COMMENT ON TABLE event_snapshot_state IS
    'R11/R15: the most recently accepted snapshot payload for an event, verbatim, plus when it arrived -- last_synced_at is what the public page''s "may be delayed" indicator (R15) is computed from.';

-- R16/R17 audit trail, mirroring entry_audit_log's shape (actor, timestamp, reason) per the
-- plan's explicit instruction.
CREATE TABLE device_loss_audit (
    id              bigserial PRIMARY KEY,
    event_id        bigint NOT NULL REFERENCES events(id),
    admin_user_id   bigint NOT NULL REFERENCES users(id),
    instance_id     varchar(64),
    reason          text,
    created_at      timestamptz NOT NULL
);

CREATE INDEX idx_device_loss_audit_event_id ON device_loss_audit(event_id);

COMMENT ON TABLE device_loss_audit IS
    'Mandatory audit record for a device-loss declaration (R16) -- an irreversible, day-affecting action gated to the ADMIN role.';
