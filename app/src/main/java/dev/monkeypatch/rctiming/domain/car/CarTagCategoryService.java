package dev.monkeypatch.rctiming.domain.car;

import dev.monkeypatch.rctiming.api.admin.dto.CarTagCategoryDto;
import dev.monkeypatch.rctiming.api.admin.dto.CreateCarTagCategoryRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class CarTagCategoryService {

    private final CarTagCategoryRepository carTagCategoryRepository;

    public CarTagCategoryService(CarTagCategoryRepository carTagCategoryRepository) {
        this.carTagCategoryRepository = carTagCategoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CarTagCategoryDto> findAll() {
        return carTagCategoryRepository.findAllByOrderBySortOrderAsc().stream()
                .map(CarTagCategoryDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CarTagCategoryDto findById(Long id) {
        return CarTagCategoryDto.from(getCategoryOrThrow(id));
    }

    public CarTagCategoryDto create(CreateCarTagCategoryRequest request) {
        CarTagCategory category = new CarTagCategory();
        category.setName(request.name());
        category.setSortOrder(request.sortOrder() != null ? request.sortOrder().shortValue() : (short) 0);
        category.setCreatedAt(Instant.now());
        return CarTagCategoryDto.from(carTagCategoryRepository.save(category));
    }

    public CarTagCategoryDto update(Long id, CreateCarTagCategoryRequest request) {
        CarTagCategory category = getCategoryOrThrow(id);
        category.setName(request.name());
        if (request.sortOrder() != null) {
            category.setSortOrder(request.sortOrder().shortValue());
        }
        return CarTagCategoryDto.from(carTagCategoryRepository.save(category));
    }

    public void delete(Long id) {
        if (!carTagCategoryRepository.existsById(id)) {
            throw new EntityNotFoundException("Car tag category not found: " + id);
        }
        carTagCategoryRepository.deleteById(id);
    }

    private CarTagCategory getCategoryOrThrow(Long id) {
        return carTagCategoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Car tag category not found: " + id));
    }
}
