-- V17: Phase 4 schema — Round, Race, RaceEntry; EventClass finals config
ALTER TABLE event_classes
    ADD COLUMN finals_count      int,
    ADD COLUMN cars_per_final    int,
    ADD COLUMN bump_count        int;

CREATE TABLE rounds (
    id                bigserial PRIMARY KEY,
    event_id          bigint NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    type              varchar(20) NOT NULL CHECK (type IN ('PRACTICE','QUALIFIER','FINAL')),
    round_number      int NOT NULL,
    sequence_in_event int NOT NULL,
    status            varchar(20) NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING','RUNNING','COMPLETED')),
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE races (
    id                  bigserial PRIMARY KEY,
    round_id            bigint NOT NULL REFERENCES rounds(id) ON DELETE CASCADE,
    event_class_id      bigint NOT NULL REFERENCES event_classes(id),
    heat_number         int NOT NULL,
    sequence_in_round   int NOT NULL,
    final_letter        varchar(5),
    start_type          varchar(20) NOT NULL CHECK (start_type IN ('STAGGER','GRID')),
    format_id           bigint REFERENCES race_format_templates(id),
    format_overrides    jsonb,
    status              varchar(20) NOT NULL DEFAULT 'PENDING'
                          CHECK (status IN ('PENDING','GRID','RUNNING','STOPPED','FINISHED')),
    started_at          timestamptz,
    finished_at         timestamptz,
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE race_entries (
    id            bigserial PRIMARY KEY,
    race_id       bigint NOT NULL REFERENCES races(id) ON DELETE CASCADE,
    entry_id      bigint NOT NULL REFERENCES entries(id),
    grid_position int,
    bumped        boolean NOT NULL DEFAULT false,
    UNIQUE (race_id, entry_id)
);

CREATE INDEX idx_rounds_event_id ON rounds(event_id);
CREATE INDEX idx_races_round_id ON races(round_id);
CREATE INDEX idx_races_event_class_id ON races(event_class_id);
CREATE INDEX idx_race_entries_race_id ON race_entries(race_id);
CREATE INDEX idx_race_entries_entry_id ON race_entries(entry_id);
