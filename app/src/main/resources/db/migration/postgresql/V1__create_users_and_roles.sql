create table users (
    id            bigserial    primary key,
    email         varchar(255) not null unique,
    password_hash varchar(255) not null,
    first_name    varchar(100) not null,
    last_name     varchar(100) not null,
    created_at    timestamptz  not null default now(),
    updated_at    timestamptz  not null default now()
);

create table user_roles (
    user_id bigint      not null references users(id) on delete cascade,
    role    varchar(50) not null,
    primary key (user_id, role)
);

create table refresh_tokens (
    id          bigserial    primary key,
    user_id     bigint       not null references users(id) on delete cascade,
    token_hash  varchar(64)  not null unique,
    expires_at  timestamptz  not null,
    created_at  timestamptz  not null default now(),
    revoked     boolean      not null default false
);

create table password_reset_tokens (
    id         bigserial    primary key,
    user_id    bigint       not null references users(id) on delete cascade,
    token_hash varchar(64)  not null unique,
    expires_at timestamptz  not null,
    created_at timestamptz  not null default now(),
    used       boolean      not null default false
);

create index idx_refresh_tokens_user_id on refresh_tokens(user_id);
create index idx_password_reset_tokens_user_id on password_reset_tokens(user_id);
