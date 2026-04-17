-- V13: Create entries table (Phase 2, RACER-11, RACER-12, RACER-13, RACER-14)

create table entries (
    id                       bigserial     primary key,
    user_id                  bigint        not null references users(id),
    event_id                 bigint        not null references events(id),
    event_class_id           bigint        references event_classes(id) on delete set null,
    transponder_number       varchar(20)   not null,   -- snapshot at submission (RACER-07)
    transponder_label        varchar(100),              -- snapshot at submission
    status                   varchar(20)   not null default 'PENDING'
                                           check (status in ('PENDING','CONFIRMED','WITHDRAWN')),
    membership_override      boolean       not null default false,  -- RACER-14
    membership_override_by   bigint        references users(id),
    membership_override_at   timestamptz,
    membership_override_note text,
    submitted_at             timestamptz   not null default now(),
    updated_at               timestamptz   not null default now()
);

create index idx_entries_user_id on entries(user_id);
create index idx_entries_event_id on entries(event_id);

-- Partial unique index: prevents duplicate entries except when status = 'WITHDRAWN' (RACER-13)
create unique index idx_entries_no_duplicate on entries(user_id, event_id, event_class_id)
    where status != 'WITHDRAWN';
