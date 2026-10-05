package dev.monkeypatch.rctiming.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    /** Officials holding a role who can still sign in, such as the enabled admins (#61). */
    @Query("select count(u) from User u where :role member of u.roles and u.disabledAt is null")
    long countEnabledWithRole(@Param("role") Role role);
}
