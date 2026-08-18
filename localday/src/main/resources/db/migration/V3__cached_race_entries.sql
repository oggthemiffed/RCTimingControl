-- V3: cached_race_entries — join between a race/heat (cached_schedule) and the entries
-- (cached_entries) participating in it, with a grid position.
--
-- Local analog of the cloud's race_entries table, needed for U4's multi-round grid
-- progression (applyPreviousRoundFinishingOrder) and finals bump-up seeding
-- (seedFinals/applyBumpUpResults). cached_entry_id is nullable — unlike the cloud's
-- entryId == 0L sentinel, an unfilled bump slot is represented as NULL here.
--
-- All DDL below is plain, transactionally-safe DDL (no CREATE INDEX CONCURRENTLY, no
-- non-transactional ALTER TYPE ... ADD VALUE) per the constraint established in V1.

CREATE TABLE cached_race_entries (
    id                  bigserial PRIMARY KEY,
    cached_schedule_id  bigint NOT NULL REFERENCES cached_schedule(id),
    cached_entry_id     bigint REFERENCES cached_entries(id),
    grid_position       int,
    car_number          int,
    bumped              boolean NOT NULL DEFAULT false
);

CREATE INDEX idx_cached_race_entries_schedule_id ON cached_race_entries(cached_schedule_id);
