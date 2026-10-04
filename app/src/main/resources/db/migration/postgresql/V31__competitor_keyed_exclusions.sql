-- V31: Championships are scored per competitor (L5, #13), so exclusions point at a competitor
-- instead of a login user. The column keeps its name: a "driver" is now a competitor.

-- Entries written between V30 and the application change (or by seed scripts that run after
-- V30) may still lack a competitor. Give them one, with the same key V30 uses.
insert into competitors (display_name, external_source, external_id, created_at, updated_at)
select distinct trim(u.first_name || ' ' || u.last_name), 'RCTC_USER', u.id::text, now(), now()
from users u
join entries e on e.user_id = u.id
where e.competitor_id is null
on conflict (external_source, external_id) do nothing;

update entries e
set competitor_id = c.id
from competitors c
where e.competitor_id is null
  and c.external_source = 'RCTC_USER'
  and c.external_id = e.user_id::text;

-- Every excluded user needs a competitor before the exclusion can point at it.
insert into competitors (display_name, external_source, external_id, created_at, updated_at)
select distinct trim(u.first_name || ' ' || u.last_name), 'RCTC_USER', u.id::text, now(), now()
from users u
join championship_exclusions x on x.driver_id = u.id
on conflict (external_source, external_id) do nothing;

alter table championship_exclusions drop constraint championship_exclusions_driver_id_fkey;

update championship_exclusions x
set driver_id = c.id
from competitors c
where c.external_source = 'RCTC_USER'
  and c.external_id = x.driver_id::text;

alter table championship_exclusions
    add constraint championship_exclusions_driver_id_fkey
    foreign key (driver_id) references competitors(id) on delete cascade;
