-- A transponder swapped on the day survives a re-import (#50). When the imported file carries a different
-- number for a swapped slot, the local number is kept and the file's number is recorded here, so officials
-- can see the difference. Nothing is blocked by it.
--   imported_transponder_number: the file's primary number, when the primary was swapped on the day and the
--     file's number differs from the one kept. Null when there is no difference.
--   imported_secondary_transponder_number: the same for the secondary slot.
ALTER TABLE entries ADD COLUMN imported_transponder_number VARCHAR(20);
ALTER TABLE entries ADD COLUMN imported_secondary_transponder_number VARCHAR(20);
