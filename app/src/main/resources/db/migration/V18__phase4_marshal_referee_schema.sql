-- V18: Phase 4 marshal adjustments, absences, incidents, penalties
CREATE TABLE marshal_adjustments (
    id                   bigserial PRIMARY KEY,
    race_id              bigint NOT NULL REFERENCES races(id),
    entry_id             bigint NOT NULL REFERENCES entries(id),
    transponder_number   varchar(20) NOT NULL,
    lap_delta            int NOT NULL CHECK (lap_delta IN (-1, 1)),
    race_state_at_time   varchar(20) NOT NULL,
    acting_user_id       bigint NOT NULL,
    acting_user_name     varchar(200) NOT NULL,
    adjusted_at          timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE marshal_absences (
    id              bigserial PRIMARY KEY,
    race_id         bigint NOT NULL REFERENCES races(id),
    entry_id        bigint NOT NULL REFERENCES entries(id),
    event_id        bigint NOT NULL REFERENCES events(id),
    recorded_at     timestamptz NOT NULL DEFAULT now(),
    recorded_by     bigint NOT NULL,
    UNIQUE (race_id, entry_id)
);

CREATE TABLE marshal_penalties (
    id              bigserial PRIMARY KEY,
    absence_id      bigint REFERENCES marshal_absences(id),
    entry_id        bigint NOT NULL REFERENCES entries(id),
    event_id        bigint NOT NULL REFERENCES events(id),
    applied_by      bigint NOT NULL,
    applied_at      timestamptz NOT NULL DEFAULT now(),
    notes           text
);

CREATE TABLE incident_reports (
    id              bigserial PRIMARY KEY,
    race_id         bigint NOT NULL REFERENCES races(id),
    entry_id        bigint NOT NULL REFERENCES entries(id),
    incident_type   varchar(50) NOT NULL,
    description     text,
    raised_by       bigint NOT NULL,
    raised_at       timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE penalties (
    id              bigserial PRIMARY KEY,
    race_id         bigint NOT NULL REFERENCES races(id),
    entry_id        bigint NOT NULL REFERENCES entries(id),
    penalty_type    varchar(20) NOT NULL CHECK (penalty_type IN ('LAP','TIME')),
    value           numeric NOT NULL,
    reason          text,
    applied_by      bigint NOT NULL,
    applied_at      timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE unknown_transponder_links (
    id                  bigserial PRIMARY KEY,
    race_id             bigint NOT NULL REFERENCES races(id),
    transponder_number  varchar(20) NOT NULL,
    linked_entry_id     bigint REFERENCES entries(id),
    linked_by           bigint NOT NULL,
    linked_at           timestamptz NOT NULL DEFAULT now(),
    UNIQUE (race_id, transponder_number)
);

CREATE INDEX idx_marshal_adjustments_race_id ON marshal_adjustments(race_id);
CREATE INDEX idx_marshal_absences_event_id ON marshal_absences(event_id);
CREATE INDEX idx_penalties_race_id ON penalties(race_id);
CREATE INDEX idx_incident_reports_race_id ON incident_reports(race_id);
CREATE INDEX idx_unknown_transponder_links_race_id ON unknown_transponder_links(race_id);
