-- V34: When an event's entries were last imported from RaceHub, and which export revision (L8, #16).
alter table events add column racehub_last_import_at timestamptz;
alter table events add column racehub_last_revision  bigint;
