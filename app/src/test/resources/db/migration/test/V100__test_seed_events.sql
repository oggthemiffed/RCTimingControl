-- V100: Test seed — events and event_classes for integration tests (Phase 2, RACER-11)
-- This migration runs only in test context (src/test/resources/db/migration/test/)

-- Insert test events with explicit IDs so tests can reference them
insert into events (id, name, event_date, status, created_at, updated_at) values
    (1001, 'Test Open Event',  current_date + interval '1 day',  'OPEN',  now(), now()),
    (1002, 'Test Draft Event', current_date + interval '30 days', 'DRAFT', now(), now());

-- Reset sequence to avoid collision with auto-generated IDs
select setval('events_id_seq', 2000, true);

-- Insert event_classes linked to the OPEN test event
-- event_class 2001: no membership requirement (open entry)
-- event_class 2002: requires BRCA membership (RACER-14 hard block test)
insert into event_classes (id, event_id, config_snapshot, config_override, template_id, created_at, updated_at) values
    (2001, 1001, '{"name":"Stock Buggy","format":"qualification"}',  null, null, now(), now()),
    (2002, 1001, '{"name":"Mod Truggy","format":"qualification"}',   null, null, now(), now());

update event_classes set required_governing_body_code = 'BRCA' where id = 2002;

-- Reset sequence to avoid collision
select setval('event_classes_id_seq', 3000, true);
