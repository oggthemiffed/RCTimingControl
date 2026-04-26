package dev.monkeypatch.rctiming.forwarder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ForwarderTokenRepository extends JpaRepository<ForwarderToken, Long> {

    Optional<ForwarderToken> findFirstByStatusOrderByGeneratedAtDesc(ForwarderTokenStatus status);

    List<ForwarderToken> findAllByStatus(ForwarderTokenStatus status);

    Optional<ForwarderToken> findFirstByOrderByGeneratedAtDesc();
}
