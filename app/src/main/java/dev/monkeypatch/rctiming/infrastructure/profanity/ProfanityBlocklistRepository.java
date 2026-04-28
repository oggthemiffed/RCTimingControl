package dev.monkeypatch.rctiming.infrastructure.profanity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface ProfanityBlocklistRepository extends JpaRepository<ProfanityBlocklistEntry, Long> {

    @Query("SELECT p.word FROM ProfanityBlocklistEntry p")
    List<String> findAllWords();

    Optional<ProfanityBlocklistEntry> findByWordIgnoreCase(String word);
}
