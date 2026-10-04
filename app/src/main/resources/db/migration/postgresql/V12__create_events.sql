-- V12: Create events table and link to event_classes (Phase 2, EVENT-03, EVENT-04)

create table events (
    id          bigserial     primary key,
    name        varchar(255)  not null,
    event_date  date          not null,
    status      varchar(20)   not null default 'DRAFT'
                              check (status in ('DRAFT','PUBLISHED','OPEN','ENTRIES_CLOSED','IN_PROGRESS','COMPLETED')),
    created_at  timestamptz   not null default now(),
    updated_at  timestamptz   not null default now()
);

-- Link event_classes to events (backfill Phase 1 table, nullable — Phase 3 will make NOT NULL)
alter table event_classes add column event_id bigint references events(id) on delete cascade;

create index idx_events_status on events(status);
create index idx_event_classes_event_id on event_classes(event_id);
