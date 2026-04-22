-- Fix V1001: seed had wrong qualifyingType enum values (BEST_LAP, BEST_CONSECUTIVE_3)
-- and wrong field names for BUMP_UP/POINTS_FINALS configs.
delete from race_format_templates;

insert into race_format_templates (name, config) values
    ('Standard Timed — 5 min',
     '{"type":"TIMED","durationMinutes":5,"startType":"ROLLING","qualifyingType":"FASTEST_LAP","racePaddingMinutes":1,"staggerIntervalSeconds":5}'),
    ('Standard Timed — 10 min',
     '{"type":"TIMED","durationMinutes":10,"startType":"ROLLING","qualifyingType":"CONSECUTIVE_LAPS","racePaddingMinutes":2,"staggerIntervalSeconds":5}'),
    ('Bump-Up Format',
     '{"type":"BUMP_UP","qualifyingHeats":3,"heatDurationMinutes":5,"bestHeatsCount":2,"gridSize":10,"bumpSpots":3,"qualifyingStartType":"STAGGER","finalsStartType":"GRID","qualifyingType":"FTQ","racePaddingMinutes":2,"staggerIntervalSeconds":5}'),
    ('Points Finals',
     '{"type":"POINTS_FINALS","qualifyingHeats":3,"finalsCount":3,"finalDurationMinutes":5,"heatDurationMinutes":5,"qualifyingStartType":"STAGGER","finalsStartType":"GRID","qualifyingType":"FTQ","racePaddingMinutes":2,"staggerIntervalSeconds":5}');
