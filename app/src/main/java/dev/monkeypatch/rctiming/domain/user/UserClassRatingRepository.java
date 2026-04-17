package dev.monkeypatch.rctiming.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserClassRatingRepository extends JpaRepository<UserClassRating, UserClassRating.UserClassRatingId> {

    List<UserClassRating> findByUserId(Long userId);
}
