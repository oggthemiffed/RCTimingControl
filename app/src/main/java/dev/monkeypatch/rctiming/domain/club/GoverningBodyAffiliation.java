package dev.monkeypatch.rctiming.domain.club;

import dev.monkeypatch.rctiming.persistence.CreatedAt;
import java.time.Instant;

public class GoverningBodyAffiliation implements CreatedAt {

    private Long id;
    private String code;
    private String displayName;
    private boolean membershipRequired = false;
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public boolean isMembershipRequired() { return membershipRequired; }
    public void setMembershipRequired(boolean membershipRequired) { this.membershipRequired = membershipRequired; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
