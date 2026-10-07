-- A refresh token is rotated on every refresh, so one sign-in becomes a chain of tokens. The family id ties
-- the chain together so signing out can revoke all of it in one statement, including a token issued by a
-- refresh that was in flight. Tokens issued before this migration have no family; each stands alone.
ALTER TABLE refresh_tokens ADD COLUMN family_id VARCHAR(36);
