-- V36: Check-in desk on the main app (L11, #19).

-- Check-in on the day is recorded here and is authoritative. RaceHub's racehub_arrival is
-- shown alongside it, read-only. A null checked_in_at means the entry has not checked in.
alter table entries add column checked_in_at         timestamptz;
alter table entries add column checked_in_by_user_id bigint references users(id) on delete set null;
