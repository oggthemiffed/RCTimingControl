-- V11: Create transponders table (Phase 2, RACER-05, RACER-06, RACER-07)

create table transponders (
    id                  bigserial     primary key,
    user_id             bigint        not null references users(id) on delete cascade,
    transponder_number  varchar(20)   not null unique,   -- system-wide uniqueness (RACER-05)
    label               varchar(100),
    created_at          timestamptz   not null default now()
);

create index idx_transponders_user_id on transponders(user_id);
create index idx_transponders_number on transponders(transponder_number);
