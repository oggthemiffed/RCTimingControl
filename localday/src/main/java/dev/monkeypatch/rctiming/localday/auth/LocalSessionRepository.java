package dev.monkeypatch.rctiming.localday.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocalSessionRepository extends JpaRepository<LocalSession, Long> {

    Optional<LocalSession> findBySessionToken(String sessionToken);
}
