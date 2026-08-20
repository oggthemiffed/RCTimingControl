-- V6: race_results — persisted final-position snapshot for a finished race (U9).
--
-- The live position machinery (LapTimingService/LiveRaceState) is deliberately in-memory-only
-- while a race is running, per the "do not store live race positions during a race" rule. This
-- table is the one durable exception: when a race transitions to FINISHED, RaceControlController
-- captures the in-memory snapshot one time and writes it here so the anonymous board API
-- (U9's BoardController) and any later results view can read a finished race's results without
-- needing the in-memory state to still be around.
--
-- All DDL below is plain, transactionally-safe DDL (no CREATE INDEX CONCURRENTLY, no
-- non-transactional ALTER TYPE ... ADD VALUE) per the constraint established in V1.

CREATE TABLE race_results (
    id                  bigserial PRIMARY KEY,
    race_id             bigint NOT NULL REFERENCES cached_schedule(id),
    entry_id            bigint NOT NULL REFERENCES cached_entries(id),
    position             int NOT NULL,
    laps_completed        int NOT NULL,
    best_lap_ms           bigint,
    recorded_at           timestamptz NOT NULL
);

CREATE INDEX idx_race_results_race_id ON race_results(race_id);
