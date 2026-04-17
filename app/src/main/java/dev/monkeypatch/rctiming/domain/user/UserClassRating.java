package dev.monkeypatch.rctiming.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "user_class_ratings")
@IdClass(UserClassRating.UserClassRatingId.class)
public class UserClassRating {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "racing_class_id")
    private Long racingClassId;

    @Column(nullable = false)
    private Short rating;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getRacingClassId() { return racingClassId; }
    public void setRacingClassId(Long racingClassId) { this.racingClassId = racingClassId; }

    public Short getRating() { return rating; }
    public void setRating(Short rating) { this.rating = rating; }

    public static class UserClassRatingId implements Serializable {

        private Long userId;
        private Long racingClassId;

        public UserClassRatingId() {}

        public UserClassRatingId(Long userId, Long racingClassId) {
            this.userId = userId;
            this.racingClassId = racingClassId;
        }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public Long getRacingClassId() { return racingClassId; }
        public void setRacingClassId(Long racingClassId) { this.racingClassId = racingClassId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UserClassRatingId that)) return false;
            return Objects.equals(userId, that.userId) && Objects.equals(racingClassId, that.racingClassId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, racingClassId);
        }
    }
}
