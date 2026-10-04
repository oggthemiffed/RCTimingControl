-- SQLite baseline (#26): tracks, racing classes, race formats, events and their classes.
-- Type conventions are listed in V1.
CREATE TABLE tracks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(255) NOT NULL,
    venue_notes TEXT,
    track_length REAL,
    created_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    updated_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER))
);

CREATE TABLE decoder_loops (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    track_id BIGINT NOT NULL,
    loop_id VARCHAR(50) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    loop_type VARCHAR(50) NOT NULL DEFAULT 'FINISH_LINE',
    is_scoring_loop BOOLEAN NOT NULL DEFAULT 1 CHECK (is_scoring_loop IN (0, 1)),
    created_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    CONSTRAINT decoder_loops_track_id_fkey FOREIGN KEY (track_id) REFERENCES tracks(id) ON DELETE CASCADE
);

CREATE TABLE racing_classes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    updated_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    CONSTRAINT racing_classes_name_key UNIQUE (name)
);

CREATE TABLE track_lap_thresholds (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    track_id BIGINT NOT NULL,
    racing_class_id BIGINT,
    min_lap_ms INTEGER NOT NULL,
    max_last_lap_ms INTEGER,
    created_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    CONSTRAINT track_lap_thresholds_track_id_racing_class_id_key UNIQUE (track_id, racing_class_id),
    CONSTRAINT fk_threshold_racing_class FOREIGN KEY (racing_class_id) REFERENCES racing_classes(id) ON DELETE SET NULL,
    CONSTRAINT track_lap_thresholds_track_id_fkey FOREIGN KEY (track_id) REFERENCES tracks(id) ON DELETE CASCADE
);

CREATE TABLE race_format_templates (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(255) NOT NULL,
    config TEXT NOT NULL CHECK (json_valid(config)),
    created_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    updated_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER))
);

CREATE TABLE events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(255) NOT NULL,
    event_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    updated_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    entry_opens_at BIGINT,
    entry_closes_at BIGINT,
    track_id BIGINT,
    racehub_last_import_at BIGINT,
    racehub_last_revision BIGINT,
    CONSTRAINT events_status_check CHECK (status IN ('DRAFT', 'PUBLISHED', 'OPEN', 'ENTRIES_CLOSED', 'IN_PROGRESS', 'COMPLETED')),
    CONSTRAINT events_track_id_fkey FOREIGN KEY (track_id) REFERENCES tracks(id) ON DELETE SET NULL
);
CREATE INDEX idx_events_status ON events (status);
CREATE INDEX idx_events_track_id ON events (track_id);

CREATE TABLE event_classes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    config_snapshot TEXT NOT NULL CHECK (json_valid(config_snapshot)),
    config_override TEXT CHECK (json_valid(config_override)),
    template_id BIGINT,
    created_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    updated_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    event_id BIGINT,
    racing_class_id BIGINT,
    combined_race_group BIGINT,
    finals_count INTEGER,
    cars_per_final INTEGER,
    bump_count INTEGER,
    CONSTRAINT event_classes_event_id_fkey FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT event_classes_racing_class_id_fkey FOREIGN KEY (racing_class_id) REFERENCES racing_classes(id) ON DELETE SET NULL,
    CONSTRAINT event_classes_template_id_fkey FOREIGN KEY (template_id) REFERENCES race_format_templates(id) ON DELETE SET NULL
);
CREATE INDEX idx_event_classes_combined_race_group ON event_classes (combined_race_group);
CREATE INDEX idx_event_classes_event_id ON event_classes (event_id);
CREATE INDEX idx_event_classes_racing_class_id ON event_classes (racing_class_id);

