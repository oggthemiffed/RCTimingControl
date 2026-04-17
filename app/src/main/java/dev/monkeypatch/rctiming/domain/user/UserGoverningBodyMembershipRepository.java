package dev.monkeypatch.rctiming.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserGoverningBodyMembershipRepository extends JpaRepository<UserGoverningBodyMembership, Long> {

    List<UserGoverningBodyMembership> findByUserId(Long userId);

    Optional<UserGoverningBodyMembership> findByUserIdAndGoverningBodyCode(Long userId, String governingBodyCode);
}
