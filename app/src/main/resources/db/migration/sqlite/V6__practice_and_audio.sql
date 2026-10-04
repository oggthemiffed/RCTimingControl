-- SQLite baseline (#26): open practice and the announcer's profanity blocklist.
-- Type conventions are listed in V1.
CREATE TABLE practice_sessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(200) NOT NULL,
    event_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'IDLE',
    best_lap_n INTEGER NOT NULL DEFAULT 3,
    created_by_user_id BIGINT,
    started_at BIGINT,
    stopped_at BIGINT,
    created_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    updated_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    CONSTRAINT practice_sessions_created_by_user_id_fkey FOREIGN KEY (created_by_user_id) REFERENCES users(id),
    CONSTRAINT practice_sessions_event_id_fkey FOREIGN KEY (event_id) REFERENCES events(id)
);
CREATE INDEX idx_practice_sessions_event ON practice_sessions (event_id) WHERE event_id IS NOT NULL;

CREATE TABLE practice_laps (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    practice_session_id BIGINT NOT NULL,
    transponder_number VARCHAR(50) NOT NULL,
    user_id BIGINT,
    lap_number INTEGER NOT NULL,
    lap_time_ms BIGINT NOT NULL,
    crossing_time BIGINT NOT NULL,
    created_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    CONSTRAINT practice_laps_practice_session_id_fkey FOREIGN KEY (practice_session_id) REFERENCES practice_sessions(id),
    CONSTRAINT practice_laps_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE INDEX idx_practice_laps_session ON practice_laps (practice_session_id);
CREATE INDEX idx_practice_laps_transponder ON practice_laps (transponder_number);

CREATE TABLE profanity_blocklist (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    word VARCHAR(200) NOT NULL,
    added_by_user_id BIGINT,
    added_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    CONSTRAINT profanity_blocklist_word_key UNIQUE (word),
    CONSTRAINT profanity_blocklist_added_by_user_id_fkey FOREIGN KEY (added_by_user_id) REFERENCES users(id)
);
CREATE UNIQUE INDEX idx_profanity_word_lower ON profanity_blocklist (lower(word));

