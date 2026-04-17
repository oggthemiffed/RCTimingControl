-- V10: Create car_tag_categories and car_tag_values tables (Phase 2, RACER-10, D-11)

create table car_tag_categories (
    id          bigserial     primary key,
    name        varchar(100)  not null unique,
    sort_order  smallint      not null default 0,
    created_at  timestamptz   not null default now()
);

-- Default categories (D-11) in priority order
insert into car_tag_categories (name, sort_order) values
    ('Chassis', 1),
    ('ESC', 2),
    ('Motor', 3),
    ('Servo', 4),
    ('Battery', 5),
    ('Body', 6),
    ('Tyres', 7);

create table car_tag_values (
    id           bigserial  primary key,
    car_id       bigint     not null references cars(id) on delete cascade,
    category_id  bigint     not null references car_tag_categories(id) on delete cascade,
    value        text       not null,
    unique (car_id, category_id)
);

create index idx_ctv_car_id on car_tag_values(car_id);
