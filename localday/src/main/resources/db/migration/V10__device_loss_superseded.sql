-- V10: device-loss supersession flag on day_lifecycle_state (U12).
--
-- Set once this instance's snapshot push is rejected with a 409 (KTD4: this instance's
-- generation is strictly lower than the cloud's stored value for the day — a replacement
-- instance has since opened at a higher generation, per a device-loss declaration, R16/R17).
-- Permanent for the rest of this local session, same "no un-supersede path" reasoning as R17's
-- cloud-side incomplete-data flag: a superseded instance never resumes automatic sync, and the
-- local UI surfaces this rather than silently discarding captured data with no explanation.
--
-- All DDL below is plain, transactionally-safe DDL per the constraint established in V1.

ALTER TABLE day_lifecycle_state ADD COLUMN superseded boolean NOT NULL DEFAULT false;
ALTER TABLE day_lifecycle_state ADD COLUMN superseded_at timestamptz;
