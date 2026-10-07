-- Who changed what on a competitor (#119 follow-up). Race directors and referees may now fix how a name is
-- said, as well as admins, so each change is recorded.
--   action: SPOKEN_NAME_CHANGED.
--   before_value / after_value: the spoken name before and after; null means none.
-- A merge moves the duplicate's rows to the competitor that was kept.
CREATE TABLE competitor_audit_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    competitor_id BIGINT NOT NULL,
    actor_user_id BIGINT NOT NULL,
    action VARCHAR(40) NOT NULL,
    before_value TEXT,
    after_value TEXT,
    created_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    CONSTRAINT competitor_audit_log_competitor_id_fkey FOREIGN KEY (competitor_id) REFERENCES competitors(id),
    CONSTRAINT competitor_audit_log_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES users(id)
);
CREATE INDEX idx_competitor_audit_log_competitor_id ON competitor_audit_log (competitor_id);
