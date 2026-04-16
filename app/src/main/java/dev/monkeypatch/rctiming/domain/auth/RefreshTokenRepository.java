package dev.monkeypatch.rctiming.domain.auth;

import dev.monkeypatch.rctiming.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void deleteByUser(User user);

    List<RefreshToken> findByUserAndRevokedFalse(User user);
}
