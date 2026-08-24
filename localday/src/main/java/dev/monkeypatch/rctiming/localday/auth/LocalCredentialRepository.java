package dev.monkeypatch.rctiming.localday.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocalCredentialRepository extends JpaRepository<LocalCredential, Long> {

    /**
     * Used by {@code daylifecycle.DayLifecycleService} (U10) to find-or-create the
     * {@link LocalCredential} row a cloud-minted official credential maps to, so a mid-day
     * pre-cache refresh upserts rather than duplicates.
     */
    Optional<LocalCredential> findByCloudUserId(Long cloudUserId);
}
