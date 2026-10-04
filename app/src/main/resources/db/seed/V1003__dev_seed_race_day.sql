-- V1003: Dev seed — full race day for UAT testing
-- Creates event, event_class, competitors, entries, rounds, races, race_entries for UAT

-- ── Race director account ────────────────────────────────────────────────────
insert into users (email, password_hash, first_name, last_name, created_at, updated_at) values
    ('director@example.com','$2b$10$QWNPqLhXElyx9PhFCXKZsOWMudKPFrvHBdP.wnQa92WYoP5Trg9oe', 'Race',    'Director', now(), now())
on conflict (email) do nothing;

-- Password above is: Racer1Pass!

insert into user_roles (user_id, role)
select id, 'RACE_DIRECTOR' from users where email = 'director@example.com'
on conflict do nothing;

insert into user_roles (user_id, role)
select id, 'REFEREE' from users where email = 'director@example.com'
on conflict do nothing;

-- ── Club profile ─────────────────────────────────────────────────────────────
insert into club_profiles (name, email, timezone, created_at, updated_at)
values ('Glasgow RC', 'info@glasgowrc.example.com', 'Europe/London', now(), now())
on conflict do nothing;

-- ── Event ─────────────────────────────────────────────────────────────────────
insert into events (name, event_date, status, track_id, created_at, updated_at)
values ('Club Championship Round 1', current_date, 'IN_PROGRESS', 1, now(), now());

-- ── Event class (Mod Buggy, Standard Timed — 5 min) ──────────────────────────
insert into event_classes (event_id, config_snapshot, template_id, racing_class_id, finals_count, cars_per_final, bump_count, created_at, updated_at)
select
    1,
    t.config,
    t.id,
    (select id from racing_classes where name = 'Mod Buggy'),
    1, 8, 0,
    now(), now()
from race_format_templates t
where t.name = 'Standard Timed — 5 min';

-- ── Competitors (six walk-in drivers; racers have no accounts, L10 #18) ─────
insert into competitors (display_name, created_at, updated_at) values
    ('Racer One',    now(), now()),
    ('Racer Two',    now(), now()),
    ('Dave Harris',  now(), now()),
    ('Chris Webb',   now(), now()),
    ('Tom Clarke',   now(), now()),
    ('Phil Evans',   now(), now());

-- ── Entries (one per competitor, transponders 101–106) ───────────────────────
insert into entries (competitor_id, event_id, event_class_id, transponder_number, transponder_label, status, submitted_at, updated_at)
select
    c.id,
    1,
    1,
    (100 + row_number() over (order by c.id))::varchar,
    'AMB-' || (100 + row_number() over (order by c.id)),
    'CONFIRMED',
    now(), now()
from competitors c
where c.display_name in ('Racer One','Racer Two','Dave Harris','Chris Webb','Tom Clarke','Phil Evans')
  and c.external_source is null
on conflict do nothing;

-- Reset entries sequence past inserted rows
select setval('entries_id_seq', (select max(id) + 100 from entries), true);

-- ── Rounds: P1, P2, P3, Q1, Q2, Q3, Final ────────────────────────────────────
insert into rounds (event_id, type, round_number, sequence_in_event, status, created_at, updated_at) values
    (1, 'PRACTICE',  1, 1, 'COMPLETED', now(), now()),
    (1, 'PRACTICE',  2, 2, 'COMPLETED', now(), now()),
    (1, 'QUALIFIER', 1, 3, 'COMPLETED', now(), now()),
    (1, 'QUALIFIER', 2, 4, 'COMPLETED', now(), now()),
    (1, 'QUALIFIER', 3, 5, 'RUNNING',   now(), now()),
    (1, 'FINAL',     1, 6, 'PENDING',   now(), now());

-- Reset rounds sequence
select setval('rounds_id_seq', (select max(id) + 100 from rounds), true);

-- ── Races: one heat per round (all 6 fit in a single heat of 8) ───────────────
insert into races (round_id, event_class_id, heat_number, sequence_in_round, final_letter, start_type, format_id, status, created_at, updated_at)
select
    r.id,
    1,        -- event_class
    1,        -- heat_number
    1,        -- sequence_in_round
    case when r.type = 'FINAL' then 'A' else null end,
    'STAGGER',
    (select id from race_format_templates where name = 'Standard Timed — 5 min'),
    case
        when r.sequence_in_event < 5 then 'FINISHED'
        when r.sequence_in_event = 5 then 'PENDING'   -- Q3 Heat 1 is the active race
        else 'PENDING'
    end,
    now(), now()
from rounds r
where r.event_id = 1
order by r.sequence_in_event;

-- Reset races sequence
select setval('races_id_seq', (select max(id) + 100 from races), true);

-- ── Race entries: put all 6 drivers in every race ─────────────────────────────
insert into race_entries (race_id, entry_id, grid_position, bumped)
select
    ra.id as race_id,
    e.id  as entry_id,
    row_number() over (partition by ra.id order by e.id) as grid_position,
    false
from races ra
cross join entries e
where e.event_class_id = 1
  and e.event_id = 1
on conflict (race_id, entry_id) do nothing;
