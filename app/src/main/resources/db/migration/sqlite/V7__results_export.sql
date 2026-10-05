-- Results Export v1 to RaceHub (#27).
--
-- events.racehub_event_id: the RaceHub event an import came from, so results can be sent back to it.
-- events.results_export_revision: the last revision handed out for the event's results export; each
--   new export takes the next number, so RaceHub can keep the highest it has seen.
-- entries.racehub_event_class_id: the RaceHub event_class_id an imported entry was booked in, so
--   result rows can name it.
-- races.abandoned_at: set when race control abandons a race, which otherwise finishes like any other.
-- results_outbox: exports waiting to be sent to RaceHub, and the outcome of each attempt.
ALTER TABLE events ADD COLUMN racehub_event_id VARCHAR(100);
ALTER TABLE events ADD COLUMN results_export_revision BIGINT NOT NULL DEFAULT 0;

ALTER TABLE entries ADD COLUMN racehub_event_class_id VARCHAR(100);

ALTER TABLE races ADD COLUMN abandoned_at BIGINT;

CREATE TABLE results_outbox (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id BIGINT NOT NULL,
    revision BIGINT NOT NULL,
    reason VARCHAR(30) NOT NULL,
    payload TEXT NOT NULL CHECK (json_valid(payload)),
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at BIGINT NOT NULL,
    last_error TEXT,
    created_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    sent_at BIGINT,
    CONSTRAINT results_outbox_reason_check CHECK (reason IN ('RACE_FINISHED', 'CORRECTION', 'DAY_CLOSE')),
    CONSTRAINT results_outbox_status_check CHECK (status IN ('QUEUED', 'FAILED', 'SENT', 'SUPERSEDED')),
    CONSTRAINT results_outbox_event_revision_key UNIQUE (event_id, revision),
    CONSTRAINT results_outbox_event_id_fkey FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
);
CREATE INDEX idx_results_outbox_due ON results_outbox (status, next_attempt_at);
