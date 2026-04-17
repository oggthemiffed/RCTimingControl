-- V8: Create user_class_ratings table (Phase 2, RACER-04)

create table user_class_ratings (
    user_id          bigint   not null references users(id) on delete cascade,
    racing_class_id  bigint   not null references racing_classes(id) on delete cascade,
    rating           smallint not null default 0 check (rating between 0 and 100),
    primary key (user_id, racing_class_id)
);
