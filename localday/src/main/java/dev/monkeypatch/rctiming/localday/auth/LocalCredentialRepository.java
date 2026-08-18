package dev.monkeypatch.rctiming.localday.auth;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalCredentialRepository extends JpaRepository<LocalCredential, Long> {
}
