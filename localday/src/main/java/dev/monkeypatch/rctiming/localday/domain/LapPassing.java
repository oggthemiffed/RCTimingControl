package dev.monkeypatch.rctiming.localday.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Durable local lap-timing storage. This is the system of record while the venue is offline:
 * every raw passing received from the decoder is written here before anything is aggregated
 * or broadcast, so a crash never loses a passing that already reached this application.
 *
 * <p>{@link #cachedScheduleId} is nullable because a passing can arrive before the operator has
 * associated it with a specific race/heat (e.g. practice, or a decoder still transmitting between
 * races) — it is resolved and backfilled by later processing, not required at insert time.
 */
@Entity
@Table(name = "lap_passings")
public class LapPassing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cached_schedule_id")
    private Long cachedScheduleId;

    @Column(name = "transponder_number", nullable = false, length = 20)
    private String transponderNumber;

    @Column(name = "passing_at", nullable = false)
    private Instant passingAt;

    @Column(name = "lap_time_ms")
    private Long lapTimeMs;

    @Column(name = "lap_number")
    private Integer lapNumber;

    @Column(name = "raw_decoder_line")
    private String rawDecoderLine;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCachedScheduleId() { return cachedScheduleId; }
    public void setCachedScheduleId(Long cachedScheduleId) { this.cachedScheduleId = cachedScheduleId; }

    public String getTransponderNumber() { return transponderNumber; }
    public void setTransponderNumber(String transponderNumber) { this.transponderNumber = transponderNumber; }

    public Instant getPassingAt() { return passingAt; }
    public void setPassingAt(Instant passingAt) { this.passingAt = passingAt; }

    public Long getLapTimeMs() { return lapTimeMs; }
    public void setLapTimeMs(Long lapTimeMs) { this.lapTimeMs = lapTimeMs; }

    public Integer getLapNumber() { return lapNumber; }
    public void setLapNumber(Integer lapNumber) { this.lapNumber = lapNumber; }

    public String getRawDecoderLine() { return rawDecoderLine; }
    public void setRawDecoderLine(String rawDecoderLine) { this.rawDecoderLine = rawDecoderLine; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
