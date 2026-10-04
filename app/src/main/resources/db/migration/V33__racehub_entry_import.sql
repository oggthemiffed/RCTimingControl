-- V33: Import entries from RaceHub Entry Export v1 (L7, #15).

-- An imported entry keeps RaceHub's id and version, so a replayed export is a no-op and only
-- a newer version of an entry changes it.
alter table entries add column external_source        varchar(30);
alter table entries add column external_entry_id      varchar(100);
alter table entries add column external_entry_version bigint;
-- RaceHub's attendance flag (NOT_ARRIVED / ARRIVED), shown read-only. Check-in here is separate.
alter table entries add column racehub_arrival        varchar(20);

create unique index idx_entries_external on entries(external_source, external_entry_id)
    where external_entry_id is not null;

-- Override for RaceHub classes whose rc_class_name does not match an event class by name.
create table racehub_class_mappings (
    id                      bigserial     primary key,
    event_id                bigint        not null references events(id) on delete cascade,
    racehub_event_class_id  varchar(100)  not null,
    event_class_id          bigint        not null references event_classes(id) on delete cascade,
    created_at              timestamptz   not null default now(),
    updated_at              timestamptz   not null default now(),
    constraint uq_racehub_class_mappings unique (event_id, racehub_event_class_id)
);
