-- Pulling an event's entries from a URL (#42). RCTC runs on a venue laptop other systems usually can't
-- reach, so it fetches Entry Export v1 from the booking system rather than waiting to be sent it.
--
-- entry_feeds: one per event.
--   url: where the Entry Export v1 document is fetched from.
--   token_encrypted: the access token sent as a bearer token, encrypted with a key kept in the data
--     folder (entry-feed-key), so a copy of the database alone doesn't give it away. Null when the URL
--     needs no token.
--   token_hint: the token's last four characters, so an official can tell which one is saved. The token
--     itself is never shown again.
--   auto_fetch: fetch every few minutes while the laptop is online, applying what imports cleanly.
--   last_fetch_at, last_status, last_error: the outcome of the latest fetch, shown on the event.
--     last_status is APPLIED, UNCHANGED, WAITING (a fetched file waits for an official to confirm it),
--     AUTH_FAILED or FAILED.
--   applied_revision: the revision of the last file applied, so a fetch of the same revision is a no-op.
--   held_document, held_revision: a fetched file waiting for an official, either from "Fetch now" or
--     from an automatic fetch that something would block.
CREATE TABLE entry_feeds (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id BIGINT NOT NULL,
    url VARCHAR(2000) NOT NULL,
    token_encrypted TEXT,
    token_hint VARCHAR(4),
    auto_fetch BOOLEAN NOT NULL DEFAULT 0 CHECK (auto_fetch IN (0, 1)),
    last_fetch_at BIGINT,
    last_status VARCHAR(20)
        CONSTRAINT entry_feeds_last_status_check
        CHECK (last_status IN ('APPLIED', 'UNCHANGED', 'WAITING', 'AUTH_FAILED', 'FAILED')),
    last_error TEXT,
    applied_revision BIGINT,
    held_document TEXT CHECK (held_document IS NULL OR json_valid(held_document)),
    held_revision BIGINT,
    created_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    updated_at BIGINT NOT NULL DEFAULT (CAST(unixepoch('subsec') * 1000000 AS INTEGER)),
    CONSTRAINT entry_feeds_event_id_key UNIQUE (event_id),
    CONSTRAINT entry_feeds_event_id_fkey FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
);
