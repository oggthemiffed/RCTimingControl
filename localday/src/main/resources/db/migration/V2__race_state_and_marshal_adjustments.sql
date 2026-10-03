-- V2: Race lifecycle timestamps on cached_schedule, plus marshal lap adjustments.
--
-- Adds started_at/finished_at to cached_schedule (the local analog of the cloud's Race
-- entity — see U3 plan unit) so the local race state machine can record when a race actually
-- started/finished, and a marshal_adjustments table mirroring the cloud's MarshalAdjustment
-- entity: a non-state-transition audit record of a marshal lap adjustment (+1/-1) applied to
-- an entry within a race.
--
-- All DDL below is plain, transactionally-safe DDL (no CREATE INDEX CONCURRENTLY, no
-- non-transactional ALTER TYPE ... ADD VALUE) per the constraint established in V1.

ALTER TABLE cached_schedule ADD COLUMN started_at timestamptz;
ALTER TABLE cached_schedule ADD COLUMN finished_at timestamptz;

-- Marshal lap adjustment audit records. race_id/entry_id reference local rows
-- (cached_schedule.id / cached_entries.id), not cloud IDs.
CREATE TABLE marshal_adjustments (
    id                  bigserial PRIMARY KEY,
    race_id             bigint NOT NULL REFERENCES cached_schedule(id),
    entry_id            bigint NOT NULL REFERENCES cached_entries(id),
    transponder_number  varchar(20) NOT NULL,
    lap_delta           int NOT NULL,
    race_state_at_time  varchar(20) NOT NULL,
    acting_user_id      bigint NOT NULL,
    acting_user_name    varchar(200) NOT NULL,
    adjusted_at         timestamptz NOT NULL
);

CREATE INDEX idx_marshal_adjustments_race_id ON marshal_adjustments(race_id);
