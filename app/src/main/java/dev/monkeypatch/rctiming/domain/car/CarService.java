package dev.monkeypatch.rctiming.domain.car;

import dev.monkeypatch.rctiming.api.racer.dto.CarDto;
import dev.monkeypatch.rctiming.api.racer.dto.CreateCarRequest;
import dev.monkeypatch.rctiming.api.racer.dto.UpdateCarRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class CarService {

    private final CarRepository carRepository;
    private final CarTagValueRepository carTagValueRepository;
    private final CarTagCategoryRepository carTagCategoryRepository;

    public CarService(CarRepository carRepository,
                      CarTagValueRepository carTagValueRepository,
                      CarTagCategoryRepository carTagCategoryRepository) {
        this.carRepository = carRepository;
        this.carTagValueRepository = carTagValueRepository;
        this.carTagCategoryRepository = carTagCategoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CarDto> findActiveByUser(Long userId) {
        return carRepository.findByUserIdAndArchivedFalse(userId).stream()
                .map(CarDto::from)
                .toList();
    }

    public CarDto create(Long userId, CreateCarRequest request) {
        Car car = new Car();
        car.setUserId(userId);
        car.setName(request.name());
        car.setPrimaryClassId(request.primaryClassId());
        car.setNotes(request.notes());
        car.setArchived(false);
        Instant now = Instant.now();
        car.setCreatedAt(now);
        car.setUpdatedAt(now);
        return CarDto.from(carRepository.save(car));
    }

    public CarDto update(Long carId, Long userId, UpdateCarRequest request) {
        Car car = getCarOrThrow(carId, userId);
        if (request.name() != null) {
            car.setName(request.name());
        }
        if (request.primaryClassId() != null) {
            car.setPrimaryClassId(request.primaryClassId());
        }
        if (request.notes() != null) {
            car.setNotes(request.notes());
        }
        car.setUpdatedAt(Instant.now());
        return CarDto.from(carRepository.save(car));
    }

    public void archive(Long carId, Long userId) {
        Car car = getCarOrThrow(carId, userId);
        car.setArchived(true);
        car.setUpdatedAt(Instant.now());
        // @Transactional dirty tracking saves automatically
    }

    public void setTag(Long carId, Long userId, Long categoryId, String value) {
        Car car = getCarOrThrow(carId, userId);
        // Verify the category exists
        if (!carTagCategoryRepository.existsById(categoryId)) {
            throw new EntityNotFoundException("Car tag category not found: " + categoryId);
        }
        carTagValueRepository.findByCar_IdAndCategoryId(carId, categoryId)
                .ifPresentOrElse(
                        existing -> existing.setValue(value),
                        () -> {
                            CarTagValue tagValue = new CarTagValue();
                            tagValue.setCar(car);
                            tagValue.setCategoryId(categoryId);
                            tagValue.setValue(value);
                            carTagValueRepository.save(tagValue);
                        });
    }

    public void deleteTag(Long carId, Long userId, Long categoryId) {
        getCarOrThrow(carId, userId);
        carTagValueRepository.findByCar_IdAndCategoryId(carId, categoryId)
                .ifPresent(carTagValueRepository::delete);
    }

    private Car getCarOrThrow(Long carId, Long userId) {
        return carRepository.findById(carId)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new EntityNotFoundException("Car not found: " + carId));
    }
}
