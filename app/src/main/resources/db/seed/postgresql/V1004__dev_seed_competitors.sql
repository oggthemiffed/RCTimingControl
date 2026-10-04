-- V1004: Dev seed — competitors for the seeded entries (L5, #13).
-- On a fresh dev database V1003 runs after the V30/V31 backfills, so its entries have no
-- competitor yet. Same key as V30: one RCTC_USER competitor per racer login.

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
