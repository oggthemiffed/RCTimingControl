package dev.monkeypatch.rctiming.domain.entryfeed;

/** The outcome of an entry feed's latest fetch (#42). */
public enum EntryFeedStatus {
    /** A new revision was imported. */
    APPLIED,
    /** The revision fetched was the one already applied, so nothing changed. */
    UNCHANGED,
    /** A fetched file waits for an official to confirm it: from "Fetch now", or something would block it. */
    WAITING,
    /** The URL refused the token. */
    AUTH_FAILED,
    /** The URL couldn't be reached, or didn't answer with an entry file. */
    FAILED
}
