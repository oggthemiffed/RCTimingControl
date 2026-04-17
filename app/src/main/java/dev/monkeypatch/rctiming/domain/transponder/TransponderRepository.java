package dev.monkeypatch.rctiming.domain.transponder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransponderRepository extends JpaRepository<Transponder, Long> {

    List<Transponder> findByUserId(Long userId);

    Optional<Transponder> findByTransponderNumber(String transponderNumber);
}
