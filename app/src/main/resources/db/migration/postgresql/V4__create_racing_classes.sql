create table racing_classes (
    id          bigserial    primary key,
    name        varchar(255) not null unique,
    description text,
    created_at  timestamptz  not null default now(),
    updated_at  timestamptz  not null default now()
);

alter table track_lap_thresholds
    add constraint fk_threshold_racing_class
    foreign key (racing_class_id) references racing_classes(id) on delete set null;
