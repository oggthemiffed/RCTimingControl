package dev.monkeypatch.rctiming.domain;

/**
 * The {@code external_source} values RCTC writes on entries and competitors it imports. A RaceHub-format file
 * can name another source of its own; walk-ins have none.
 */
public final class ExternalSources {

    /** RaceHub's Entry Export, and a file in its format that names no source. */
    public static final String RACEHUB = "RACEHUB";

    /** An RC-Timing style driver CSV. */
    public static final String CSV = "CSV";

    private ExternalSources() {
    }
}
