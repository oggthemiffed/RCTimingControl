-- V9: Create cars table (Phase 2, RACER-08, RACER-09, RACER-10)

create table cars (
    id               bigserial     primary key,
    user_id          bigint        not null references users(id) on delete cascade,
    name             varchar(100)  not null,
    primary_class_id bigint        references racing_classes(id) on delete set null,
    notes            text,
    archived         boolean       not null default false,
    created_at       timestamptz   not null default now(),
    updated_at       timestamptz   not null default now()
);

create index idx_cars_user_id on cars(user_id);
