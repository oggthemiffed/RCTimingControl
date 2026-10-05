-- Live feed for remote viewers (#28).
--
-- events.live_feed_enabled: whether race control sends this event's live timing to the relay while
--   its races run. Off by default; race control turns it on per event.
ALTER TABLE events ADD COLUMN live_feed_enabled BOOLEAN NOT NULL DEFAULT 0 CHECK (live_feed_enabled IN (0, 1));
