package dev.monkeypatch.rctiming.domain.club;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubProfileRepository extends JpaRepository<ClubProfile, Long> {
    // Singleton-pattern entity — service enforces at most one profile.
    // Use findAll() + check size, or findFirst() to retrieve the singleton.
}
