package dev.monkeypatch.rctiming.api.racer;

import dev.monkeypatch.rctiming.api.racer.dto.CarDto;
import dev.monkeypatch.rctiming.api.racer.dto.CreateCarRequest;
import dev.monkeypatch.rctiming.api.racer.dto.SetCarTagRequest;
import dev.monkeypatch.rctiming.api.racer.dto.UpdateCarRequest;
import dev.monkeypatch.rctiming.domain.car.CarService;
import dev.monkeypatch.rctiming.query.car.CarQueryService;
import dev.monkeypatch.rctiming.query.car.CarWithTagsDto;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/racer/cars")
@PreAuthorize("hasRole('RACER')")
public class CarController {

    private final CarService carService;
    private final CarQueryService carQueryService;

    public CarController(CarService carService, CarQueryService carQueryService) {
        this.carService = carService;
        this.carQueryService = carQueryService;
    }

    @GetMapping
    public List<CarWithTagsDto> listCars(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return carQueryService.getActiveCarsForUser(userId);
    }

    @GetMapping("/{id}")
    public CarWithTagsDto getCar(@PathVariable Long id, Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return carQueryService.getCarForUser(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Car not found: " + id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CarDto createCar(Authentication auth, @RequestBody @Valid CreateCarRequest request) {
        Long userId = Long.parseLong(auth.getName());
        return carService.create(userId, request);
    }

    @PatchMapping("/{id}")
    public CarDto updateCar(@PathVariable Long id,
                             Authentication auth,
                             @RequestBody @Valid UpdateCarRequest request) {
        Long userId = Long.parseLong(auth.getName());
        return carService.update(id, userId, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveCar(@PathVariable Long id, Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        carService.archive(id, userId);
    }

    @PostMapping("/{id}/tags")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setTag(@PathVariable Long id,
                       Authentication auth,
                       @RequestBody @Valid SetCarTagRequest request) {
        Long userId = Long.parseLong(auth.getName());
        carService.setTag(id, userId, request.categoryId(), request.value());
    }

    @DeleteMapping("/{id}/tags/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTag(@PathVariable Long id,
                          @PathVariable Long categoryId,
                          Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        carService.deleteTag(id, userId, categoryId);
    }
}
