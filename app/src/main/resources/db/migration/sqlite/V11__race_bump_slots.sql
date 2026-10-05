-- Bump slots in finals (#45).
--
-- races.bump_slots: how many places at the back of this final are kept for drivers bumped up from
--   the final below. Set when finals are seeded and filled when the lower final finishes. Zero for
--   heats and for the lowest final. race_entries rows always point at a real entry, so an unfilled
--   slot is this count, not a placeholder row.
ALTER TABLE races ADD COLUMN bump_slots INTEGER NOT NULL DEFAULT 0;
