-- V1001: Dev seed — racing classes and race format templates

insert into tracks (name, venue_notes, track_length) values
    ('Main Circuit', 'Outdoor all-weather surface. Pit lane on the east side.', 120.5),
    ('Indoor Carpet Track', 'Climate-controlled. Carpet surface, low-grip setup recommended.', 85.0);

insert into racing_classes (name, description) values
    ('Stock Buggy',      '2WD buggy, stock motor class'),
    ('Mod Buggy',        '2WD buggy, modified motor class'),
    ('Stock Truggy',     '1/8 truggy, stock spec class'),
    ('Mod Truggy',       '1/8 truggy, modified class'),
    ('Short Course',     '2WD short course trucks'),
    ('Electric On-Road', 'Electric touring car / on-road class');

insert into race_format_templates (name, config) values
    ('Standard Timed — 5 min',
     '{"type":"TIMED","durationMinutes":5,"startType":"ROLLING","qualifyingType":"BEST_LAP","racePaddingMinutes":1,"staggerIntervalSeconds":0}'),
    ('Standard Timed — 10 min',
     '{"type":"TIMED","durationMinutes":10,"startType":"ROLLING","qualifyingType":"BEST_CONSECUTIVE_3","racePaddingMinutes":2,"staggerIntervalSeconds":0}'),
    ('Bump-Up Format',
     '{"type":"BUMP_UP","durationMinutes":5,"heats":3,"startType":"ROLLING","qualifyingType":"BEST_LAP","racePaddingMinutes":1,"staggerIntervalSeconds":0}'),
    ('Points Finals',
     '{"type":"POINTS_FINALS","durationMinutes":10,"startType":"ROLLING","qualifyingType":"BEST_CONSECUTIVE_3","racePaddingMinutes":2,"staggerIntervalSeconds":0}');
