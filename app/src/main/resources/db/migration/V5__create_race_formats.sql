create table race_format_templates (
    id         bigserial    primary key,
    name       varchar(255) not null,
    config     jsonb        not null,
    created_at timestamptz  not null default now(),
    updated_at timestamptz  not null default now()
);

create table event_classes (
    id              bigserial    primary key,
    config_snapshot jsonb        not null,
    config_override jsonb,
    template_id     bigint       references race_format_templates(id) on delete set null,
    created_at      timestamptz  not null default now(),
    updated_at      timestamptz  not null default now()
);
