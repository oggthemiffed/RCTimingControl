-- SQLite baseline (#26): competitors, entries and the RaceHub import mapping.
-- Type conventions are listed in V1.
CREATE TABLE competitors (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    display_name VARCHAR(255) NOT NULL,
    external_source VARCHAR(50),
    external_id VARCHAR(100),
    brca_number VARCHAR(50),
    home_club VARCHAR(255),
    created_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    updated_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    CONSTRAINT uq_competitors_external UNIQUE (external_source, external_id)
);

CREATE TABLE entries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id BIGINT,
    event_id BIGINT NOT NULL,
    event_class_id BIGINT,
    transponder_number VARCHAR(20) NOT NULL,
    transponder_label VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    submitted_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    updated_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    confirmed_at BIGINT,
    withdrawn_at BIGINT,
    competitor_id BIGINT,
    secondary_transponder_number VARCHAR(20),
    external_source VARCHAR(30),
    external_entry_id VARCHAR(100),
    external_entry_version BIGINT,
    racehub_arrival VARCHAR(20),
    checked_in_at BIGINT,
    checked_in_by_user_id BIGINT,
    CONSTRAINT entries_status_check CHECK (status IN ('PENDING', 'CONFIRMED', 'WITHDRAWN')),
    CONSTRAINT entries_checked_in_by_user_id_fkey FOREIGN KEY (checked_in_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT entries_competitor_id_fkey FOREIGN KEY (competitor_id) REFERENCES competitors(id),
    CONSTRAINT entries_event_class_id_fkey FOREIGN KEY (event_class_id) REFERENCES event_classes(id) ON DELETE SET NULL,
    CONSTRAINT entries_event_id_fkey FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT entries_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE INDEX idx_entries_competitor_id ON entries (competitor_id);
CREATE INDEX idx_entries_event_id ON entries (event_id);
CREATE UNIQUE INDEX idx_entries_external ON entries (external_source, external_entry_id) WHERE external_entry_id IS NOT NULL;
CREATE UNIQUE INDEX idx_entries_no_duplicate ON entries (competitor_id, event_id, event_class_id) WHERE status <> 'WITHDRAWN';
CREATE INDEX idx_entries_user_id ON entries (user_id);

CREATE TABLE entry_audit_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    entry_id BIGINT NOT NULL,
    admin_user_id BIGINT NOT NULL,
    action VARCHAR(40) NOT NULL,
    reason TEXT,
    before_snapshot TEXT,
    after_snapshot TEXT,
    created_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    CONSTRAINT entry_audit_log_admin_user_id_fkey FOREIGN KEY (admin_user_id) REFERENCES users(id),
    CONSTRAINT entry_audit_log_entry_id_fkey FOREIGN KEY (entry_id) REFERENCES entries(id) ON DELETE CASCADE
);
CREATE INDEX idx_entry_audit_log_entry_id ON entry_audit_log (entry_id);

CREATE TABLE racehub_class_mappings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id BIGINT NOT NULL,
    racehub_event_class_id VARCHAR(100) NOT NULL,
    event_class_id BIGINT NOT NULL,
    created_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    updated_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    CONSTRAINT uq_racehub_class_mappings UNIQUE (event_id, racehub_event_class_id),
    CONSTRAINT racehub_class_mappings_event_class_id_fkey FOREIGN KEY (event_class_id) REFERENCES event_classes(id) ON DELETE CASCADE,
    CONSTRAINT racehub_class_mappings_event_id_fkey FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
);

