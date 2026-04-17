package dev.monkeypatch.rctiming.api.admin.dto;

import dev.monkeypatch.rctiming.domain.car.CarTagCategory;

public record CarTagCategoryDto(
        Long id,
        String name,
        int sortOrder) {

    public static CarTagCategoryDto from(CarTagCategory category) {
        return new CarTagCategoryDto(
                category.getId(),
                category.getName(),
                category.getSortOrder());
    }
}
