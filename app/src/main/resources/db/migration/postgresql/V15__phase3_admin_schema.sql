-- V15: Phase 3 column additions (EVENT-07 track FK, EVENT-02 racing_class FK,
--      EVENT-06 combined race grouping, D-21 CarTagCategory soft-delete, D-22 MinIO logo URL).

alter table events
    add column track_id bigint references tracks(id) on delete set null;

alter table event_classes
    add column racing_class_id bigint references racing_classes(id) on delete set null,
    add column combined_race_group bigint;

alter table club_profiles
    add column logo_url varchar(500);

alter table car_tag_categories
    add column archived boolean not null default false;

create index idx_events_track_id on events(track_id);
create index idx_event_classes_racing_class_id on event_classes(racing_class_id);
create index idx_event_classes_combined_race_group on event_classes(combined_race_group);
create index idx_car_tag_categories_archived on car_tag_categories(archived);
