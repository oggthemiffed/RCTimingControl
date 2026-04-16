package dev.monkeypatch.rctiming.domain.club;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoverningBodyAffiliationRepository extends JpaRepository<GoverningBodyAffiliation, Long> {

    Optional<GoverningBodyAffiliation> findByCode(String code);

    boolean existsByCode(String code);
}
