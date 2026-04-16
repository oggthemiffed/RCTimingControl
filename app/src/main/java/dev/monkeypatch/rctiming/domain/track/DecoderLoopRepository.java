package dev.monkeypatch.rctiming.domain.track;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DecoderLoopRepository extends JpaRepository<DecoderLoop, Long> {

    List<DecoderLoop> findByTrackId(Long trackId);
}
