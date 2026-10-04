-- SQLite baseline (#26): result snapshots and championships.
-- Type conventions are listed in V1.
CREATE TABLE result_snapshots (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    race_id BIGINT NOT NULL,
    finished_at BIGINT NOT NULL,
    positions_json TEXT NOT NULL CHECK (json_valid(positions_json)),
    lap_history_json TEXT NOT NULL CHECK (json_valid(lap_history_json)),
    created_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    CONSTRAINT result_snapshots_race_id_key UNIQUE (race_id),
    CONSTRAINT result_snapshots_race_id_fkey FOREIGN KEY (race_id) REFERENCES races(id) ON DELETE CASCADE
);
CREATE INDEX idx_result_snapshots_race_id ON result_snapshots (race_id);

CREATE TABLE championships (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(255) NOT NULL,
    best_x_from_y_x INTEGER,
    best_x_from_y_y INTEGER,
    scoring_source VARCHAR(20) NOT NULL DEFAULT 'FINALS',
    tq_bonus_points INTEGER NOT NULL DEFAULT 0,
    afinal_winner_bonus_points INTEGER NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    updated_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    CONSTRAINT championships_scoring_source_check CHECK (scoring_source IN ('QUALIFYING', 'FINALS', 'BOTH'))
);

CREATE TABLE championship_classes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    championship_id BIGINT NOT NULL,
    racing_class_id BIGINT NOT NULL,
    best_x_from_y_x INTEGER,
    best_x_from_y_y INTEGER,
    created_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    CONSTRAINT championship_classes_championship_id_racing_class_id_key UNIQUE (championship_id, racing_class_id),
    CONSTRAINT championship_classes_championship_id_fkey FOREIGN KEY (championship_id) REFERENCES championships(id) ON DELETE CASCADE,
    CONSTRAINT championship_classes_racing_class_id_fkey FOREIGN KEY (racing_class_id) REFERENCES racing_classes(id) ON DELETE RESTRICT
);
CREATE INDEX idx_championship_classes_championship_id ON championship_classes (championship_id);

CREATE TABLE championship_event_links (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    championship_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    round_number INTEGER NOT NULL,
    created_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    CONSTRAINT championship_event_links_championship_id_event_id_key UNIQUE (championship_id, event_id),
    CONSTRAINT championship_event_links_championship_id_round_number_key UNIQUE (championship_id, round_number),
    CONSTRAINT championship_event_links_championship_id_fkey FOREIGN KEY (championship_id) REFERENCES championships(id) ON DELETE CASCADE,
    CONSTRAINT championship_event_links_event_id_fkey FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
);
CREATE INDEX idx_championship_event_links_championship_id ON championship_event_links (championship_id);

CREATE TABLE championship_points_scale (
    championship_id BIGINT NOT NULL,
    position INTEGER NOT NULL,
    points INTEGER NOT NULL,
    PRIMARY KEY (championship_id, position),
    CONSTRAINT championship_points_scale_points_check CHECK (points >= 0),
    CONSTRAINT championship_points_scale_position_check CHECK (position >= 1),
    CONSTRAINT championship_points_scale_championship_id_fkey FOREIGN KEY (championship_id) REFERENCES championships(id) ON DELETE CASCADE
);

CREATE TABLE championship_exclusions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    championship_id BIGINT NOT NULL,
    driver_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    created_by BIGINT NOT NULL,
    created_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    CONSTRAINT championship_exclusions_championship_id_fkey FOREIGN KEY (championship_id) REFERENCES championships(id) ON DELETE CASCADE,
    CONSTRAINT championship_exclusions_created_by_fkey FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT championship_exclusions_driver_id_fkey FOREIGN KEY (driver_id) REFERENCES competitors(id) ON DELETE CASCADE,
    CONSTRAINT championship_exclusions_event_id_fkey FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
);
CREATE INDEX idx_championship_exclusions_championship_id ON championship_exclusions (championship_id);
CREATE INDEX idx_championship_exclusions_driver_id ON championship_exclusions (driver_id);

