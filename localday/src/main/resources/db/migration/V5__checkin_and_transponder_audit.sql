-- V5: Check-in / attendance confirmation and transponder reassignment audit (U7).
--
-- Adds checked_in/checked_in_at to cached_entries so attendance can be confirmed at the venue
-- with zero cloud dependency (R5), plus a UNIQUE constraint on transponder_number — the
-- transponder-reassignment conflict check (R8) depends on transponder numbers being unique per
-- entry, and enforcing that invariant at the DB level (not just in application code) mirrors
-- V1's own UNIQUE (cloud_entry_id) constraint on this same table. Also creates
-- transponder_reassignment_audit, a full audit trail of reassignments tied to the acting
-- official's identity, mirroring V2's marshal_adjustments table shape.
--
-- All DDL below is plain, transactionally-safe DDL (no CREATE INDEX CONCURRENTLY, no
-- non-transactional ALTER TYPE ... ADD VALUE) per the constraint established in V1.

ALTER TABLE cached_entries ADD COLUMN checked_in boolean NOT NULL DEFAULT false;
ALTER TABLE cached_entries ADD COLUMN checked_in_at timestamptz;

ALTER TABLE cached_entries ADD CONSTRAINT uq_cached_entries_transponder_number UNIQUE (transponder_number);

-- Transponder reassignment audit records. cached_entry_id references a local row
-- (cached_entries.id), not a cloud ID.
CREATE TABLE transponder_reassignment_audit (
    id                      bigserial PRIMARY KEY,
    cached_entry_id         bigint NOT NULL REFERENCES cached_entries(id),
    old_transponder_number  varchar(20) NOT NULL,
    new_transponder_number  varchar(20) NOT NULL,
    acting_credential_id    bigint NOT NULL,
    acting_official_name    varchar(200) NOT NULL,
    reassigned_at           timestamptz NOT NULL
);

CREATE INDEX idx_transponder_reassignment_audit_cached_entry_id ON transponder_reassignment_audit(cached_entry_id);
