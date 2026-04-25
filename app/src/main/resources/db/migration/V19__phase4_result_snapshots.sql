CREATE TABLE result_snapshots (
    id              bigserial PRIMARY KEY,
    race_id         bigint NOT NULL UNIQUE REFERENCES races(id) ON DELETE CASCADE,
    finished_at     timestamptz NOT NULL,
    positions_json  jsonb NOT NULL,
    lap_history_json jsonb NOT NULL,
    created_at      timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_result_snapshots_race_id ON result_snapshots(race_id);
