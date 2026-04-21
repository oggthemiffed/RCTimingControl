-- V16: Championship tables (CHAMP-01..09).
-- CHAMP-01: best_x_from_y_x / best_x_from_y_y columns on championships
-- CHAMP-02: handled via championship_exclusions + scoring logic in query module (Phase 7)
-- CHAMP-03: championship_classes (separate standings per racing class)
-- CHAMP-04: championship_points_scale (position → points table, per RESEARCH pitfall 7)
-- CHAMP-06: scoring_source enum column
-- CHAMP-07: tq_bonus_points column
-- CHAMP-08: afinal_winner_bonus_points column
-- CHAMP-09: championship_exclusions audit table

create table championships (
    id                         bigserial    primary key,
    name                       varchar(255) not null,
    best_x_from_y_x            int,
    best_x_from_y_y            int,
    scoring_source             varchar(20)  not null default 'FINALS'
                               check (scoring_source in ('QUALIFYING','FINALS','BOTH')),
    tq_bonus_points            int          not null default 0,
    afinal_winner_bonus_points int          not null default 0,
    created_at                 timestamptz  not null default now(),
    updated_at                 timestamptz  not null default now()
);

create table championship_classes (
    id                    bigserial    primary key,
    championship_id       bigint       not null references championships(id) on delete cascade,
    racing_class_id       bigint       not null references racing_classes(id) on delete restrict,
    -- Per-class override for best-X-from-Y (D-11). Other fields inherit championship defaults.
    best_x_from_y_x       int,
    best_x_from_y_y       int,
    created_at            timestamptz  not null default now(),
    unique (championship_id, racing_class_id)
);

create table championship_event_links (
    id               bigserial    primary key,
    championship_id  bigint       not null references championships(id) on delete cascade,
    event_id         bigint       not null references events(id) on delete cascade,
    round_number     int          not null,
    created_at       timestamptz  not null default now(),
    unique (championship_id, event_id),
    unique (championship_id, round_number)
);

create table championship_points_scale (
    championship_id  bigint       not null references championships(id) on delete cascade,
    position         int          not null,
    points           int          not null,
    primary key (championship_id, position),
    check (position >= 1),
    check (points >= 0)
);

create table championship_exclusions (
    id               bigserial    primary key,
    championship_id  bigint       not null references championships(id) on delete cascade,
    driver_id        bigint       not null references users(id) on delete cascade,
    event_id         bigint       not null references events(id) on delete cascade,
    reason           text         not null,
    created_by       bigint       not null references users(id) on delete restrict,
    created_at       timestamptz  not null default now()
);

create index idx_championship_classes_championship_id on championship_classes(championship_id);
create index idx_championship_event_links_championship_id on championship_event_links(championship_id);
create index idx_championship_exclusions_championship_id on championship_exclusions(championship_id);
create index idx_championship_exclusions_driver_id on championship_exclusions(driver_id);
