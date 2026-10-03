-- V30: Competitor identity (L4, #12). A competitor keeps a driver's racing history across
-- meetings without a login. Entries point at a competitor instead of a user.

create table competitors (
    id               bigserial     primary key,
    display_name     varchar(255)  not null,
    external_source  varchar(50),       -- e.g. RACEHUB; RCTC_USER for competitors backfilled here
    external_id      varchar(100),      -- the identifier in external_source
    brca_number      varchar(50),
    home_club        varchar(255),
    created_at       timestamptz   not null default now(),
    updated_at       timestamptz   not null default now(),
    constraint uq_competitors_external unique (external_source, external_id)
);

alter table entries add column competitor_id bigint references competitors(id);

-- One competitor per existing user that has entries. The key (RCTC_USER, user id) is stable,
-- so the same competitor is found again by the application's findOrCreate.
insert into competitors (display_name, external_source, external_id, created_at, updated_at)
select trim(u.first_name || ' ' || u.last_name), 'RCTC_USER', u.id::text, now(), now()
from users u
where exists (select 1 from entries e where e.user_id = u.id);

update entries e
set competitor_id = c.id
from competitors c
where c.external_source = 'RCTC_USER'
  and c.external_id = e.user_id::text;

-- Entries can now exist without a login user (walk-ins, L9).
alter table entries alter column user_id drop not null;

-- Duplicate detection moves from the user to the competitor, with the same rule.
drop index idx_entries_no_duplicate;
create unique index idx_entries_no_duplicate on entries(competitor_id, event_id, event_class_id)
    where status != 'WITHDRAWN';

create index idx_entries_competitor_id on entries(competitor_id);
