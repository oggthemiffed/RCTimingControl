package dev.monkeypatch.rctiming.query.car;

import dev.monkeypatch.rctiming.jooq.generated.tables.CarTagCategories;
import dev.monkeypatch.rctiming.jooq.generated.tables.CarTagValues;
import dev.monkeypatch.rctiming.jooq.generated.tables.Cars;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CarQueryService {

    private static final Cars CARS = Cars.CARS;
    private static final CarTagCategories CTC = CarTagCategories.CAR_TAG_CATEGORIES;
    private static final CarTagValues CTV = CarTagValues.CAR_TAG_VALUES;

    private final DSLContext dsl;

    public CarQueryService(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<CarWithTagsDto> getActiveCarsForUser(Long userId) {
        // 1. Load active (non-archived) cars for user, ordered by creation date
        var carRows = dsl.selectFrom(CARS)
                .where(CARS.USER_ID.eq(userId).and(CARS.ARCHIVED.isFalse()))
                .orderBy(CARS.CREATED_AT.asc())
                .fetch();
        if (carRows.isEmpty()) {
            return List.of();
        }

        List<Long> carIds = carRows.map(r -> r.get(CARS.ID));

        // 2. Load tag values for those cars, joining category name (two round trips avoids Cartesian explosion)
        Map<Long, Map<String, String>> tagsByCarId = dsl
                .select(CTV.CAR_ID, CTC.NAME, CTV.VALUE)
                .from(CTV)
                .join(CTC).on(CTC.ID.eq(CTV.CATEGORY_ID))
                .where(CTV.CAR_ID.in(carIds))
                .fetchGroups(CTV.CAR_ID).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream().collect(Collectors.toMap(
                                r -> r.get(CTC.NAME),
                                r -> r.get(CTV.VALUE),
                                (a, b) -> a, LinkedHashMap::new))
                ));

        // 3. Assemble DTOs preserving car order
        return carRows.stream()
                .map(r -> new CarWithTagsDto(
                        r.get(CARS.ID),
                        r.get(CARS.USER_ID),
                        r.get(CARS.NAME),
                        r.get(CARS.PRIMARY_CLASS_ID),
                        r.get(CARS.NOTES),
                        Boolean.TRUE.equals(r.get(CARS.ARCHIVED)),
                        tagsByCarId.getOrDefault(r.get(CARS.ID), Map.of())
                ))
                .toList();
    }

    public Optional<CarWithTagsDto> getCarForUser(Long carId, Long userId) {
        var row = dsl.selectFrom(CARS)
                .where(CARS.ID.eq(carId).and(CARS.USER_ID.eq(userId)))
                .fetchOptional();
        if (row.isEmpty()) {
            return Optional.empty();
        }

        Map<String, String> tags = dsl.select(CTC.NAME, CTV.VALUE)
                .from(CTV)
                .join(CTC).on(CTC.ID.eq(CTV.CATEGORY_ID))
                .where(CTV.CAR_ID.eq(carId))
                .fetchMap(CTC.NAME, CTV.VALUE);

        var r = row.get();
        return Optional.of(new CarWithTagsDto(
                r.get(CARS.ID),
                r.get(CARS.USER_ID),
                r.get(CARS.NAME),
                r.get(CARS.PRIMARY_CLASS_ID),
                r.get(CARS.NOTES),
                Boolean.TRUE.equals(r.get(CARS.ARCHIVED)),
                tags
        ));
    }
}
