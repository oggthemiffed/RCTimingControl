package dev.monkeypatch.rctiming.persistence;

import java.time.Instant;

/**
 * An entity whose row records when it was made. {@link JooqRepository#save} sets the time when the entity has
 * none yet, so only a time that means something else (such as when an audited change happened) is set by hand.
 */
public interface CreatedAt {

    Instant getCreatedAt();

    void setCreatedAt(Instant createdAt);
}
