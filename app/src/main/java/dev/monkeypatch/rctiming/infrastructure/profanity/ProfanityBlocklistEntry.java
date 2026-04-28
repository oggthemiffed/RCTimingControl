package dev.monkeypatch.rctiming.infrastructure.profanity;

import dev.monkeypatch.rctiming.domain.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "profanity_blocklist")
public class ProfanityBlocklistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String word;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by_user_id")
    private User addedBy;

    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt = Instant.now();

    public Long getId() { return id; }

    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }

    public User getAddedBy() { return addedBy; }
    public void setAddedBy(User addedBy) { this.addedBy = addedBy; }

    public Instant getAddedAt() { return addedAt; }
}
