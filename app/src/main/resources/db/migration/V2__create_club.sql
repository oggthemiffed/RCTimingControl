create table club_profiles (
    id            bigserial    primary key,
    name          varchar(255) not null,
    email         varchar(255),
    phone         varchar(50),
    website_url   varchar(500),
    latitude      double precision,
    longitude     double precision,
    timezone      varchar(100) not null default 'UTC',
    logo          bytea,
    logo_type     varchar(10),
    created_at    timestamptz  not null default now(),
    updated_at    timestamptz  not null default now()
);

create table governing_body_affiliations (
    id                   bigserial    primary key,
    code                 varchar(50)  not null unique,
    display_name         varchar(255) not null,
    membership_required  boolean      not null default false,
    created_at           timestamptz  not null default now()
);
