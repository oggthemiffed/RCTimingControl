-- Phase 5 / TIMING-08: Audit trail for in-race retroactive transponder linking (D-12)
-- Note: V18 created unknown_transponder_links for CTRL-06 in-race registration.
-- This V22 table is the retroactive-link audit record created by TransponderLinkController
-- when race staff retroactively credit laps to an entry (REFEREE/RACE_DIRECTOR action).
CREATE TABLE unknown_transponder_link (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    race_id BIGINT NOT NULL REFERENCES races(id),
    transponder_number VARCHAR(50) NOT NULL,
    entry_id BIGINT NOT NULL REFERENCES entries(id),
    linked_by_user_id BIGINT REFERENCES users(id),
    linked_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_unknown_transponder_link_race ON unknown_transponder_link(race_id);
