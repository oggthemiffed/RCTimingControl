package dev.monkeypatch.rctiming.domain.race;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "races")
public class Race {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "round_id", nullable = false)
    private Long roundId;

    @Column(name = "event_class_id", nullable = false)
    private Long eventClassId;

    @Column(name = "heat_number", nullable = false)
    private int heatNumber;

    @Column(name = "sequence_in_round", nullable = false)
    private int sequenceInRound;

    @Column(name = "final_letter", length = 5)
    private String finalLetter;

    @Column(name = "start_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private StartType startType;

    @Column(name = "format_id")
    private Long formatId;

    @Column(name = "format_overrides", columnDefinition = "jsonb")
    private String formatOverrides;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RaceStatus status = RaceStatus.PENDING;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRoundId() { return roundId; }
    public void setRoundId(Long roundId) { this.roundId = roundId; }

    public Long getEventClassId() { return eventClassId; }
    public void setEventClassId(Long eventClassId) { this.eventClassId = eventClassId; }

    public int getHeatNumber() { return heatNumber; }
    public void setHeatNumber(int heatNumber) { this.heatNumber = heatNumber; }

    public int getSequenceInRound() { return sequenceInRound; }
    public void setSequenceInRound(int sequenceInRound) { this.sequenceInRound = sequenceInRound; }

    public String getFinalLetter() { return finalLetter; }
    public void setFinalLetter(String finalLetter) { this.finalLetter = finalLetter; }

    public StartType getStartType() { return startType; }
    public void setStartType(StartType startType) { this.startType = startType; }

    public Long getFormatId() { return formatId; }
    public void setFormatId(Long formatId) { this.formatId = formatId; }

    public String getFormatOverrides() { return formatOverrides; }
    public void setFormatOverrides(String formatOverrides) { this.formatOverrides = formatOverrides; }

    public RaceStatus getStatus() { return status; }
    public void setStatus(RaceStatus status) { this.status = status; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
