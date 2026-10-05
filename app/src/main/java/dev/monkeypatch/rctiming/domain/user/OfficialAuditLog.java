package dev.monkeypatch.rctiming.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** One change to an official's account, and who made it (#61). Never holds a password. */
@Entity
@Table(name = "official_audit_log")
public class OfficialAuditLog {

    /** What was changed. */
    public enum Action { ADDED, ROLES_CHANGED, PASSWORD_SET, DISABLED, ENABLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "official_user_id", nullable = false)
    private Long officialUserId;

    /** The admin who made the change; null when it came from the laptop's command line. */
    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(nullable = false, length = 40)
    private String action;

    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OfficialAuditLog() {
    }

    public OfficialAuditLog(Long officialUserId, Long actorUserId, Action action, String detail, Instant createdAt) {
        this.officialUserId = officialUserId;
        this.actorUserId = actorUserId;
        this.action = action.name();
        this.detail = detail;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getOfficialUserId() { return officialUserId; }
    public Long getActorUserId() { return actorUserId; }
    public String getAction() { return action; }
    public String getDetail() { return detail; }
    public Instant getCreatedAt() { return createdAt; }
}
