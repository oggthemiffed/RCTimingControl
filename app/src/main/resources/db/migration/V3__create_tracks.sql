create table tracks (
    id           bigserial    primary key,
    name         varchar(255) not null,
    venue_notes  text,
    track_length double precision,
    created_at   timestamptz  not null default now(),
    updated_at   timestamptz  not null default now()
);

create table decoder_loops (
    id              bigserial    primary key,
    track_id        bigint       not null references tracks(id) on delete cascade,
    loop_id         varchar(50)  not null,
    display_name    varchar(255) not null,
    loop_type       varchar(50)  not null default 'FINISH_LINE',
    is_scoring_loop boolean      not null default true,
    created_at      timestamptz  not null default now()
);

create table track_lap_thresholds (
    id               bigserial   primary key,
    track_id         bigint      not null references tracks(id) on delete cascade,
    racing_class_id  bigint,
    min_lap_ms       integer     not null,
    max_last_lap_ms  integer,
    created_at       timestamptz not null default now(),
    unique (track_id, racing_class_id)
);
