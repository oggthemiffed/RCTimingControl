package dev.monkeypatch.rctiming.domain.club;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "club_profiles")
public class ClubProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String email;

    private String phone;

    @Column(name = "website_url")
    private String websiteUrl;

    private Double latitude;

    private Double longitude;

    @Column(nullable = false)
    private String timezone = "UTC";

    @Column(columnDefinition = "bytea")
    private byte[] logo;

    @Column(name = "logo_type", length = 10)
    private String logoType;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "audio_settings", columnDefinition = "jsonb")
    private ClubAudioSettings audioSettings = ClubAudioSettings.defaults();

    @Column(name = "default_voice_id", length = 100)
    private String defaultVoiceId = "en_GB-alan-medium";

    @Column(name = "show_car_tags_in_results", nullable = false)
    private boolean showCarTagsInResults = false;

    @Column(name = "decoder_host", length = 255)
    private String decoderHost;

    @Column(name = "decoder_port")
    private Integer decoderPort;

    @Column(name = "decoder_protocol", length = 10)
    private String decoderProtocol;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) {
        java.time.ZoneId.of(timezone); // validate on set
        this.timezone = timezone;
    }

    public byte[] getLogo() { return logo; }
    public void setLogo(byte[] logo) { this.logo = logo; }

    public String getLogoType() { return logoType; }
    public void setLogoType(String logoType) { this.logoType = logoType; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public ClubAudioSettings getAudioSettings() { return audioSettings; }
    public void setAudioSettings(ClubAudioSettings audioSettings) { this.audioSettings = audioSettings; }

    public String getDefaultVoiceId() { return defaultVoiceId; }
    public void setDefaultVoiceId(String defaultVoiceId) { this.defaultVoiceId = defaultVoiceId; }

    public boolean isShowCarTagsInResults() { return showCarTagsInResults; }
    public void setShowCarTagsInResults(boolean showCarTagsInResults) { this.showCarTagsInResults = showCarTagsInResults; }

    public String getDecoderHost() { return decoderHost; }
    public void setDecoderHost(String decoderHost) { this.decoderHost = decoderHost; }

    public Integer getDecoderPort() { return decoderPort; }
    public void setDecoderPort(Integer decoderPort) { this.decoderPort = decoderPort; }

    public String getDecoderProtocol() { return decoderProtocol; }
    public void setDecoderProtocol(String decoderProtocol) { this.decoderProtocol = decoderProtocol; }
}
