package dev.monkeypatch.rctiming.domain.competitor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A driver who races, independent of any login. Entries point at a competitor, so a racing
 * history can outlive a single meeting and does not depend on an account (L4, #12).
 */
@Entity
@Table(name = "competitors")
public class Competitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "external_source", length = 50)
    private String externalSource;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @Column(name = "brca_number", length = 50)
    private String brcaNumber;

    @Column(name = "home_club", length = 255)
    private String homeClub;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
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
