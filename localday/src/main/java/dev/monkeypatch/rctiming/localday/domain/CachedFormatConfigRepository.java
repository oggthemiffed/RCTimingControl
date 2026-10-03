package dev.monkeypatch.rctiming.localday.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CachedFormatConfigRepository extends JpaRepository<CachedFormatConfig, Long> {

    Optional<CachedFormatConfig> findByCloudFormatId(Long cloudFormatId);
}
