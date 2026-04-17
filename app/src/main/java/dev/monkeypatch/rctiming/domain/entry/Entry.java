package dev.monkeypatch.rctiming.domain.entry;

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
@Table(name = "entries")
public class Entry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "event_class_id")
    private Long eventClassId;

    @Column(name = "car_id")
    private Long carId;

    @Column(name = "transponder_id")
    private Long transponderId;

    // Snapshot columns — captured at submit time (RACER-07)
    // V13 names these transponder_number and transponder_label (no _snapshot suffix)
    @Column(name = "transponder_number", nullable = false, length = 20)
    private String transponderNumberSnapshot;

    @Column(name = "transponder_label", length = 100)
    private String transponderLabelSnapshot;

    // Membership override — stored via membership_override_by FK column (RACER-14)
    @Column(name = "membership_override_by")
    private Long membershipOverrideByAdminId;

    @Column(name = "membership_override", nullable = false)
    private boolean membershipOverride = false;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EntryStatus status = EntryStatus.PENDING;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public Long getEventClassId() { return eventClassId; }
    public void setEventClassId(Long eventClassId) { this.eventClassId = eventClassId; }

    public Long getCarId() { return carId; }
    public void setCarId(Long carId) { this.carId = carId; }

    public Long getTransponderId() { return transponderId; }
    public void setTransponderId(Long transponderId) { this.transponderId = transponderId; }

    public String getTransponderNumberSnapshot() { return transponderNumberSnapshot; }
    public void setTransponderNumberSnapshot(String transponderNumberSnapshot) { this.transponderNumberSnapshot = transponderNumberSnapshot; }

    public String getTransponderLabelSnapshot() { return transponderLabelSnapshot; }
    public void setTransponderLabelSnapshot(String transponderLabelSnapshot) { this.transponderLabelSnapshot = transponderLabelSnapshot; }

    public Long getMembershipOverrideByAdminId() { return membershipOverrideByAdminId; }
    public void setMembershipOverrideByAdminId(Long membershipOverrideByAdminId) {
        this.membershipOverrideByAdminId = membershipOverrideByAdminId;
        this.membershipOverride = (membershipOverrideByAdminId != null);
    }

    public boolean isMembershipOverride() { return membershipOverride; }
    public void setMembershipOverride(boolean membershipOverride) { this.membershipOverride = membershipOverride; }

    public EntryStatus getStatus() { return status; }
    public void setStatus(EntryStatus status) { this.status = status; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; }

    public Instant getWithdrawnAt() { return withdrawnAt; }
    public void setWithdrawnAt(Instant withdrawnAt) { this.withdrawnAt = withdrawnAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
