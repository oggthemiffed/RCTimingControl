-- V21: Forwarder API token table (FORWARDER-05)
-- Stores BCrypt hashes of admin-generated forwarder auth tokens.
-- Plaintext is NEVER stored; returned exactly once on POST /api/v1/admin/forwarder/token.

CREATE TABLE forwarder_token (
    id            BIGSERIAL PRIMARY KEY,
    token_hash    VARCHAR(255) NOT NULL,
    status        VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE','REVOKED')),
    generated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at    TIMESTAMPTZ
);

CREATE INDEX idx_forwarder_token_status ON forwarder_token(status);
-- Only one row may have status = 'ACTIVE' at a time. Enforced at the application layer
-- (ForwarderTokenService.generate revokes any existing ACTIVE row before inserting).
-- A partial unique index is intentionally NOT used here so admin operations remain a single
-- atomic-at-the-service-level transaction without DB-side conflict edge cases.
