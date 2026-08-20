package dev.monkeypatch.rctiming.domain.localday;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocaldayCredentialRepository extends JpaRepository<LocaldayCredential, Long> {

    Optional<LocaldayCredential> findByEventIdAndUserId(Long eventId, Long userId);

    void deleteByEventIdAndUserId(Long eventId, Long userId);
}
