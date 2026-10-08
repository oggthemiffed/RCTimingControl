-- The race history (#140) reads the audit rows of one race, and refreshes every few seconds while the race runs.
CREATE INDEX idx_audit_log_race ON audit_log (race_id);
