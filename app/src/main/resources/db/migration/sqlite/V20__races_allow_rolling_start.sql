-- races.start_type now comes from the class's race format (#142), which can also say ROLLING. SQLite cannot change a
-- CHECK in place, so the table is rebuilt as SQLite's ALTER TABLE documentation describes: foreign keys off, copy
-- into a new table, drop the old one and rename the new one.
--
-- Foreign keys can only be turned off outside a transaction, and with them on, dropping races would delete the rows
-- of every table that cascades from it. So this script runs outside Flyway's transaction
-- (V20__races_allow_rolling_start.sql.conf) and wraps the rebuild in a savepoint of its own: Flyway reads a bare
-- BEGIN as the start of a block.
PRAGMA foreign_keys = OFF;

SAVEPOINT rebuild_races;

CREATE TABLE races_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    round_id BIGINT NOT NULL,
    event_class_id BIGINT NOT NULL,
    heat_number INTEGER NOT NULL,
    sequence_in_round INTEGER NOT NULL,
    final_letter VARCHAR(5),
    start_type VARCHAR(20) NOT NULL,
    format_id BIGINT,
    format_overrides TEXT CHECK (json_valid(format_overrides)),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    started_at BIGINT,
    finished_at BIGINT,
    created_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    updated_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    abandoned_at BIGINT,
    bump_slots INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT races_start_type_check CHECK (start_type IN ('STAGGER', 'GRID', 'ROLLING')),
    CONSTRAINT races_status_check CHECK (status IN ('PENDING', 'GRID', 'RUNNING', 'STOPPED', 'FINISHED')),
    CONSTRAINT races_event_class_id_fkey FOREIGN KEY (event_class_id) REFERENCES event_classes(id),
    CONSTRAINT races_format_id_fkey FOREIGN KEY (format_id) REFERENCES race_format_templates(id),
    CONSTRAINT races_round_id_fkey FOREIGN KEY (round_id) REFERENCES rounds(id) ON DELETE CASCADE
);

INSERT INTO races_new (id, round_id, event_class_id, heat_number, sequence_in_round, final_letter, start_type,
                       format_id, format_overrides, status, started_at, finished_at, created_at, updated_at,
                       abandoned_at, bump_slots)
SELECT id, round_id, event_class_id, heat_number, sequence_in_round, final_letter, start_type,
       format_id, format_overrides, status, started_at, finished_at, created_at, updated_at,
       abandoned_at, bump_slots
FROM races;

DROP TABLE races;
ALTER TABLE races_new RENAME TO races;

CREATE INDEX idx_races_event_class_id ON races (event_class_id);
CREATE INDEX idx_races_round_id ON races (round_id);

RELEASE rebuild_races;

PRAGMA foreign_keys = ON;
