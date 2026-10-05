-- Managing officials after setup (#61).
--
-- users.disabled_at: when an admin stopped this official signing in; null while they can. Officials
--   are never deleted, since audit logs point at them.
ALTER TABLE users ADD COLUMN disabled_at BIGINT;

-- official_audit_log: every change to an official's account, and who made it.
--   actor_user_id: the admin who made the change, or null when it came from the laptop's command
--     line (reset-admin-password).
--   action: ADDED, ROLES_CHANGED, PASSWORD_SET, DISABLED or ENABLED.
--   detail: what changed, for example the roles before and after. Never a password.
CREATE TABLE official_audit_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    official_user_id BIGINT NOT NULL,
    actor_user_id BIGINT,
    action VARCHAR(40) NOT NULL,
    detail TEXT,
    created_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    CONSTRAINT official_audit_log_official_user_id_fkey FOREIGN KEY (official_user_id) REFERENCES users(id),
    CONSTRAINT official_audit_log_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES users(id)
);
CREATE INDEX idx_official_audit_log_created_at ON official_audit_log (created_at);
