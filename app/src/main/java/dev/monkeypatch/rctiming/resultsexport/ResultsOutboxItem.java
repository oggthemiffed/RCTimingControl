package dev.monkeypatch.rctiming.resultsexport;

import dev.monkeypatch.rctiming.persistence.CreatedAt;
import java.time.Instant;

/** A results export waiting to go to RaceHub, or already sent (#27). */
public class ResultsOutboxItem implements CreatedAt {

    private Long id;

    private Long eventId;

    private long revision;

    private ExportReason reason;

    /** The Results Export v1 JSON, exactly as it is sent. */
    private String payload;

    private OutboxStatus status = OutboxStatus.QUEUED;

    private int attempts;

    private Instant nextAttemptAt;

    private String lastError;

    private Instant createdAt;

    private Instant sentAt;

    public Long getId() { return id; }
    void setId(Long id) { this.id = id; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public long getRevision() { return revision; }
    public void setRevision(long revision) { this.revision = revision; }

    public ExportReason getReason() { return reason; }
    public void setReason(ExportReason reason) { this.reason = reason; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public OutboxStatus getStatus() { return status; }
    public void setStatus(OutboxStatus status) { this.status = status; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(Instant nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
}
