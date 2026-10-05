-- Corrections after a race finishes (#63).
--
-- result_snapshots.timed_positions_json: the result as timed when the race finished, before
--   corrections. positions_json is the corrected result: rebuilt from this, the time penalties and
--   any lap penalties or marshal lap adjustments given after the finish, each time one is added.
--   Results pages, championship points and the RaceHub export all read positions_json.
--   Null for a snapshot stored before this: its positions are as timed, without time penalties or
--   later corrections, until the race is next corrected. The results export uses the null to say which
--   penalties such a result already allows for.
ALTER TABLE result_snapshots ADD COLUMN timed_positions_json TEXT
    CHECK (timed_positions_json IS NULL OR json_valid(timed_positions_json));
