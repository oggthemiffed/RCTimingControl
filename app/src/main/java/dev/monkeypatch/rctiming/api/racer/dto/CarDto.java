package dev.monkeypatch.rctiming.api.racer.dto;

import dev.monkeypatch.rctiming.domain.car.Car;

public record CarDto(
        Long id,
        Long userId,
        String name,
        Long primaryClassId,
        String notes,
        boolean archived) {

    public static CarDto from(Car car) {
        return new CarDto(
                car.getId(),
                car.getUserId(),
                car.getName(),
                car.getPrimaryClassId(),
                car.getNotes(),
                car.isArchived());
    }
}
