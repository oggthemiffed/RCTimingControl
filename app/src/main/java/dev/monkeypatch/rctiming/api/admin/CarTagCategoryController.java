package dev.monkeypatch.rctiming.api.admin;

import dev.monkeypatch.rctiming.api.admin.dto.CarTagCategoryDto;
import dev.monkeypatch.rctiming.api.admin.dto.CreateCarTagCategoryRequest;
import dev.monkeypatch.rctiming.domain.car.CarTagCategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/car-tag-categories")
@PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR', 'REFEREE')")
public class CarTagCategoryController {

    private final CarTagCategoryService carTagCategoryService;

    public CarTagCategoryController(CarTagCategoryService carTagCategoryService) {
        this.carTagCategoryService = carTagCategoryService;
    }

    @GetMapping
    public List<CarTagCategoryDto> listCategories() {
        return carTagCategoryService.findAll();
    }

    @GetMapping("/{id}")
    public CarTagCategoryDto getCategory(@PathVariable Long id) {
        return carTagCategoryService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CarTagCategoryDto createCategory(@RequestBody @Valid CreateCarTagCategoryRequest request) {
        return carTagCategoryService.create(request);
    }

    @PutMapping("/{id}")
    public CarTagCategoryDto updateCategory(@PathVariable Long id,
                                             @RequestBody @Valid CreateCarTagCategoryRequest request) {
        return carTagCategoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id) {
        carTagCategoryService.delete(id);
    }
}
