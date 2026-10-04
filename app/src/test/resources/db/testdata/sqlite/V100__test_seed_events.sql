-- V100: Test seed — events and event_classes for integration tests
-- This migration runs only in test context (src/test/resources/db/testdata/)

-- Insert test events with explicit IDs so tests can reference them
insert into events (id, name, event_date, status) values
    (1001, 'Test Open Event',  date('now', '+1 day'),  'OPEN'),
    (1002, 'Test Draft Event', date('now', '+30 days'), 'DRAFT');

-- Move the id sequence on so generated IDs never collide with the fixed ones
update sqlite_sequence set seq = 2000 where name = 'events';

-- Insert event_classes linked to the OPEN test event
insert into event_classes (id, event_id, config_snapshot, config_override, template_id) values
    (2001, 1001, '{"name":"Stock Buggy","format":"qualification"}',  null, null),
    (2002, 1001, '{"name":"Mod Truggy","format":"qualification"}',   null, null);

update sqlite_sequence set seq = 3000 where name = 'event_classes';
