-- V7: Create user_governing_body_memberships table (Phase 2, RACER-03)

create table user_governing_body_memberships (
    id                   bigserial     primary key,
    user_id              bigint        not null references users(id) on delete cascade,
    governing_body_code  varchar(20)   not null,
    membership_number    varchar(50)   not null,
    created_at           timestamptz   not null default now(),
    updated_at           timestamptz   not null default now(),
    unique (user_id, governing_body_code)
);

create index idx_ugbm_user_id on user_governing_body_memberships(user_id);
