-- V26: Store forwarder token plaintext for forwarder.env download (FORWARDER-05 UX fix)
-- Forwarder tokens are cryptographically random 32-byte values — they are API keys, not
-- passwords. BCrypt is inappropriate here; direct comparison against the stored random value
-- is equivalent security at a fraction of the cost, and enables embedding the token in the
-- downloaded forwarder.env without a second one-time-reveal window.
ALTER TABLE forwarder_token ADD COLUMN token_value VARCHAR(255);

-- Backfill existing rows to null — they were generated before this change.
-- Any pre-existing ACTIVE token cannot be recovered; the admin will need to regenerate.
-- token_hash is retained for audit purposes but is no longer used for validation.
