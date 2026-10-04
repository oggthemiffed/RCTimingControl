-- SQLite baseline (#26): rounds, races, grids, marshal and referee records, transponder links.
-- Type conventions are listed in V1.
CREATE TABLE rounds (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    round_number INTEGER NOT NULL,
    sequence_in_event INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    updated_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    CONSTRAINT rounds_status_check CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED')),
    CONSTRAINT rounds_type_check CHECK (type IN ('PRACTICE', 'QUALIFIER', 'FINAL')),
    CONSTRAINT rounds_event_id_fkey FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
);
CREATE INDEX idx_rounds_event_id ON rounds (event_id);

CREATE TABLE races (
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
    CONSTRAINT races_start_type_check CHECK (start_type IN ('STAGGER', 'GRID')),
    CONSTRAINT races_status_check CHECK (status IN ('PENDING', 'GRID', 'RUNNING', 'STOPPED', 'FINISHED')),
    CONSTRAINT races_event_class_id_fkey FOREIGN KEY (event_class_id) REFERENCES event_classes(id),
    CONSTRAINT races_format_id_fkey FOREIGN KEY (format_id) REFERENCES race_format_templates(id),
    CONSTRAINT races_round_id_fkey FOREIGN KEY (round_id) REFERENCES rounds(id) ON DELETE CASCADE
);
CREATE INDEX idx_races_event_class_id ON races (event_class_id);
CREATE INDEX idx_races_round_id ON races (round_id);

CREATE TABLE race_entries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    race_id BIGINT NOT NULL,
    entry_id BIGINT NOT NULL,
    grid_position INTEGER,
    bumped BOOLEAN NOT NULL DEFAULT 0 CHECK (bumped IN (0, 1)),
    car_number INTEGER,
    CONSTRAINT race_entries_race_id_entry_id_key UNIQUE (race_id, entry_id),
    CONSTRAINT race_entries_entry_id_fkey FOREIGN KEY (entry_id) REFERENCES entries(id),
    CONSTRAINT race_entries_race_id_fkey FOREIGN KEY (race_id) REFERENCES races(id) ON DELETE CASCADE
);
CREATE INDEX idx_race_entries_entry_id ON race_entries (entry_id);
CREATE INDEX idx_race_entries_race_id ON race_entries (race_id);

CREATE TABLE marshal_adjustments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    race_id BIGINT NOT NULL,
    entry_id BIGINT NOT NULL,
    transponder_number VARCHAR(20) NOT NULL,
    lap_delta INTEGER NOT NULL,
    race_state_at_time VARCHAR(20) NOT NULL,
    acting_user_id BIGINT NOT NULL,
    acting_user_name VARCHAR(200) NOT NULL,
    adjusted_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    CONSTRAINT marshal_adjustments_lap_delta_check CHECK (lap_delta IN (-1, 1)),
    CONSTRAINT marshal_adjustments_entry_id_fkey FOREIGN KEY (entry_id) REFERENCES entries(id),
    CONSTRAINT marshal_adjustments_race_id_fkey FOREIGN KEY (race_id) REFERENCES races(id)
);
CREATE INDEX idx_marshal_adjustments_race_id ON marshal_adjustments (race_id);

CREATE TABLE marshal_absences (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    race_id BIGINT NOT NULL,
    entry_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    recorded_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    recorded_by BIGINT NOT NULL,
    CONSTRAINT marshal_absences_race_id_entry_id_key UNIQUE (race_id, entry_id),
    CONSTRAINT marshal_absences_entry_id_fkey FOREIGN KEY (entry_id) REFERENCES entries(id),
    CONSTRAINT marshal_absences_event_id_fkey FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT marshal_absences_race_id_fkey FOREIGN KEY (race_id) REFERENCES races(id)
);
CREATE INDEX idx_marshal_absences_event_id ON marshal_absences (event_id);

CREATE TABLE marshal_penalties (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    absence_id BIGINT,
    entry_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    applied_by BIGINT NOT NULL,
    applied_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    notes TEXT,
    CONSTRAINT marshal_penalties_absence_id_fkey FOREIGN KEY (absence_id) REFERENCES marshal_absences(id),
    CONSTRAINT marshal_penalties_entry_id_fkey FOREIGN KEY (entry_id) REFERENCES entries(id),
    CONSTRAINT marshal_penalties_event_id_fkey FOREIGN KEY (event_id) REFERENCES events(id)
);

CREATE TABLE incident_reports (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    race_id BIGINT NOT NULL,
    entry_id BIGINT NOT NULL,
    incident_type VARCHAR(50) NOT NULL,
    description TEXT,
    raised_by BIGINT NOT NULL,
    raised_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    CONSTRAINT incident_reports_entry_id_fkey FOREIGN KEY (entry_id) REFERENCES entries(id),
    CONSTRAINT incident_reports_race_id_fkey FOREIGN KEY (race_id) REFERENCES races(id)
);
CREATE INDEX idx_incident_reports_race_id ON incident_reports (race_id);

CREATE TABLE penalties (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    race_id BIGINT NOT NULL,
    entry_id BIGINT NOT NULL,
    penalty_type VARCHAR(20) NOT NULL,
    value NUMERIC NOT NULL,
    reason TEXT,
    applied_by BIGINT NOT NULL,
    applied_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    CONSTRAINT penalties_penalty_type_check CHECK (penalty_type IN ('LAP', 'TIME')),
    CONSTRAINT penalties_entry_id_fkey FOREIGN KEY (entry_id) REFERENCES entries(id),
    CONSTRAINT penalties_race_id_fkey FOREIGN KEY (race_id) REFERENCES races(id)
);
CREATE INDEX idx_penalties_race_id ON penalties (race_id);

CREATE TABLE unknown_transponder_links (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    race_id BIGINT NOT NULL,
    transponder_number VARCHAR(20) NOT NULL,
    linked_entry_id BIGINT,
    linked_by BIGINT NOT NULL,
    linked_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    CONSTRAINT unknown_transponder_links_race_id_transponder_number_key UNIQUE (race_id, transponder_number),
    CONSTRAINT unknown_transponder_links_linked_entry_id_fkey FOREIGN KEY (linked_entry_id) REFERENCES entries(id),
    CONSTRAINT unknown_transponder_links_race_id_fkey FOREIGN KEY (race_id) REFERENCES races(id)
);
CREATE INDEX idx_unknown_transponder_links_race_id ON unknown_transponder_links (race_id);

CREATE TABLE unknown_transponder_link (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    race_id BIGINT NOT NULL,
    transponder_number VARCHAR(50) NOT NULL,
    entry_id BIGINT NOT NULL,
    linked_by_user_id BIGINT,
    linked_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    CONSTRAINT unknown_transponder_link_entry_id_fkey FOREIGN KEY (entry_id) REFERENCES entries(id),
    CONSTRAINT unknown_transponder_link_linked_by_user_id_fkey FOREIGN KEY (linked_by_user_id) REFERENCES users(id),
    CONSTRAINT unknown_transponder_link_race_id_fkey FOREIGN KEY (race_id) REFERENCES races(id)
);
CREATE INDEX idx_unknown_transponder_link_race ON unknown_transponder_link (race_id);

