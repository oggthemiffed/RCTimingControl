package dev.monkeypatch.rctiming.domain.car;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CarTagValueRepository extends JpaRepository<CarTagValue, Long> {
    List<CarTagValue> findByCar_Id(Long carId);
    Optional<CarTagValue> findByCar_IdAndCategoryId(Long carId, Long categoryId);
    void deleteByCar_IdAndCategoryId(Long carId, Long categoryId);
}
