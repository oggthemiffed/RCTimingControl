-- V1003: Dev seed — full race day for UAT testing
-- Creates event, event_class, entries, rounds, races, race_entries for UAT

-- ── Additional racer users (racer1/racer2 already exist from V1000) ──────────
insert into users (email, password_hash, first_name, last_name, created_at, updated_at) values
    ('racer3@example.com',  '$2b$10$QWNPqLhXElyx9PhFCXKZsOWMudKPFrvHBdP.wnQa92WYoP5Trg9oe', 'Dave',    'Harris',   now(), now()),
    ('racer4@example.com',  '$2b$10$QWNPqLhXElyx9PhFCXKZsOWMudKPFrvHBdP.wnQa92WYoP5Trg9oe', 'Chris',   'Webb',     now(), now()),
    ('racer5@example.com',  '$2b$10$QWNPqLhXElyx9PhFCXKZsOWMudKPFrvHBdP.wnQa92WYoP5Trg9oe', 'Tom',     'Clarke',   now(), now()),
    ('racer6@example.com',  '$2b$10$QWNPqLhXElyx9PhFCXKZsOWMudKPFrvHBdP.wnQa92WYoP5Trg9oe', 'Phil',    'Evans',    now(), now()),
    ('director@example.com','$2b$10$QWNPqLhXElyx9PhFCXKZsOWMudKPFrvHBdP.wnQa92WYoP5Trg9oe', 'Race',    'Director', now(), now())
on conflict (email) do nothing;

-- Passwords above are all: Racer1Pass!

insert into user_roles (user_id, role)
select id, 'RACER' from users where email in (
    'racer3@example.com','racer4@example.com','racer5@example.com','racer6@example.com'
) on conflict do nothing;

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

-- ── Transponders (one per racer, numbers 101–106) ────────────────────────────
insert into transponders (user_id, transponder_number, label, created_at)
select
    u.id,
    (100 + row_number() over (order by u.id))::varchar,
    'AMB-' || (100 + row_number() over (order by u.id)),
    now()
from users u
where u.email in (
    'racer1@example.com','racer2@example.com',
    'racer3@example.com','racer4@example.com',
    'racer5@example.com','racer6@example.com'
)
on conflict (transponder_number) do nothing;

-- ── Cars (one per racer, linked to their transponder) ─────────────────────────
insert into cars (user_id, name, transponder_id, primary_class_id, created_at, updated_at)
select
    u.id,
    u.first_name || '''s Buggy',
    t.id,
    (select id from racing_classes where name = 'Mod Buggy'),
    now(), now()
from users u
join transponders t on t.user_id = u.id
where u.email in (
    'racer1@example.com','racer2@example.com',
    'racer3@example.com','racer4@example.com',
    'racer5@example.com','racer6@example.com'
)
on conflict do nothing;

-- ── Entries (6 racers, car + transponder FKs properly set) ───────────────────
insert into entries (user_id, event_id, event_class_id, car_id, transponder_id, transponder_number, transponder_label, status, submitted_at, updated_at)
select
    u.id,
    1,
    1,
    c.id,
    t.id,
    t.transponder_number,
    t.label,
    'CONFIRMED',
    now(), now()
from users u
join transponders t on t.user_id = u.id
join cars c on c.user_id = u.id and c.transponder_id = t.id
where u.email in (
    'racer1@example.com','racer2@example.com',
    'racer3@example.com','racer4@example.com',
    'racer5@example.com','racer6@example.com'
)
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
