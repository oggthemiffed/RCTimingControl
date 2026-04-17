-- V14: Add entry_audit_log table and missing entry/event columns (Phase 2, plan 04)

-- Add entry entry lifecycle tracking columns to entries table
alter table entries
    add column if not exists car_id             bigint references cars(id) on delete set null,
    add column if not exists transponder_id     bigint references transponders(id) on delete set null,
    add column if not exists confirmed_at       timestamptz,
    add column if not exists withdrawn_at       timestamptz;

-- Rename snapshot columns to match plan interface (existing columns used as snapshots)
-- transponder_number and transponder_label are already the snapshot — add alias columns
-- NOTE: We keep existing column names and adapt in Java rather than renaming.

-- Add membership_override_by_admin_id alias (existing column is membership_override_by)
-- NOTE: We map to membership_override_by in the entity.

-- Add optional entry window columns to events
alter table events
    add column if not exists entry_opens_at     timestamptz,
    add column if not exists entry_closes_at    timestamptz;

-- Add required_governing_body_code to event_classes for membership enforcement (RACER-14)
alter table event_classes
    add column if not exists required_governing_body_code varchar(50);

-- Entry audit log: records admin overrides (D-12 transponder swap, D-13 membership override)
create table entry_audit_log (
    id              bigserial       primary key,
    entry_id        bigint          not null references entries(id) on delete cascade,
    admin_user_id   bigint          not null references users(id),
    action          varchar(40)     not null,   -- 'TRANSPONDER_SWAP' | 'MEMBERSHIP_OVERRIDE'
    reason          text,
    before_snapshot jsonb,
    after_snapshot  jsonb,
    created_at      timestamptz     not null default now()
);

create index idx_entry_audit_log_entry_id on entry_audit_log(entry_id);
