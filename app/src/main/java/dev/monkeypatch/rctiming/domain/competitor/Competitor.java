package dev.monkeypatch.rctiming.domain.competitor;

import java.time.Instant;

/**
 * A driver who races, independent of any login. Entries point at a competitor, so a racing
 * history can outlive a single meeting and does not depend on an account (L4, #12).
 */
public class Competitor {

    private Long id;
    private String displayName;
    private String externalSource;
    private String externalId;
    private String brcaNumber;
    private String homeClub;
    private Instant createdAt;
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getExternalSource() { return externalSource; }
    public void setExternalSource(String externalSource) { this.externalSource = externalSource; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getBrcaNumber() { return brcaNumber; }
    public void setBrcaNumber(String brcaNumber) { this.brcaNumber = brcaNumber; }

    public String getHomeClub() { return homeClub; }
    public void setHomeClub(String homeClub) { this.homeClub = homeClub; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
