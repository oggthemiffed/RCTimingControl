package dev.monkeypatch.rctiming.domain.localday;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocaldayInstanceSecretRepository extends JpaRepository<LocaldayInstanceSecret, Long> {

    Optional<LocaldayInstanceSecret> findByEventIdAndInstanceId(Long eventId, String instanceId);
}
