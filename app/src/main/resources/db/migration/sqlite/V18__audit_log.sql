-- One log for what officials (and the system) do, so there is a single place to read, filter and export it (#138).
-- The older per-subject logs (official_audit_log, entry_audit_log, competitor_audit_log) stay as they are.
--   occurred_at: UTC microseconds.
--   actor_user_id: the official, or null for the command line, a background job or someone not signed in.
--   actor_label: who that was, kept as text so the row still reads right if the official is later renamed:
--     the official's name and email, "cli:<os user>", "system:<job>" or "anonymous:<email tried>".
--   source: UI (a request through the app), CLI (a command line tool) or SYSTEM (a background job).
--   action: what happened, for example LOGIN_FAILED, in capitals.
--   entity_type / entity_id: what it happened to ("official", "17"). entity_id is text because it may be an
--     email or another key. event_id / race_id: the event or race it belongs to, when there is one. They are
--     not foreign keys, so a row outlives anything later deleted.
--   summary: one line a person can read. before_json / after_json: the values before and after, as JSON.
CREATE TABLE audit_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    occurred_at BIGINT NOT NULL,
    actor_user_id BIGINT,
    actor_label VARCHAR(200) NOT NULL,
    source VARCHAR(10) NOT NULL CHECK (source IN ('UI', 'CLI', 'SYSTEM')),
    action VARCHAR(60) NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_id VARCHAR(64),
    event_id BIGINT,
    race_id BIGINT,
    summary VARCHAR(500) NOT NULL,
    before_json TEXT CHECK (before_json IS NULL OR json_valid(before_json)),
    after_json TEXT CHECK (after_json IS NULL OR json_valid(after_json)),
    CONSTRAINT audit_log_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES users(id)
);
CREATE INDEX idx_audit_log_occurred_at ON audit_log (occurred_at);
CREATE INDEX idx_audit_log_entity ON audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_log_event ON audit_log (event_id);
