package dev.monkeypatch.rctiming.api.racer.dto;

import dev.monkeypatch.rctiming.domain.user.UserClassRating;

public record ClassRatingDto(Long racingClassId, Short rating) {

    public static ClassRatingDto from(UserClassRating r) {
        return new ClassRatingDto(r.getRacingClassId(), r.getRating());
    }
}
