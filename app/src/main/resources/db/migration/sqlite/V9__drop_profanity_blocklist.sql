-- Drops the profanity blocklist (#30). Names were screened against it when racers typed them into
-- their own profiles; racer accounts went in #18, and nothing has been checked against it since.
-- RaceHub, where racers now enter their names, is the place to screen them.
DROP TABLE profanity_blocklist;
