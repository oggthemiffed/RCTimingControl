-- How a competitor's name is said aloud (#119). Optional: when empty, the display name is spoken as written.
-- It belongs to the competitor, so a correction made once carries over to later meetings, and a RaceHub or
-- CSV re-import never touches it.
ALTER TABLE competitors ADD COLUMN spoken_name VARCHAR(255);
