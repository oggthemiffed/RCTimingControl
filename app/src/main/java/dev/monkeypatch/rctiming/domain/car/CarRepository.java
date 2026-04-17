package dev.monkeypatch.rctiming.domain.car;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarRepository extends JpaRepository<Car, Long> {
    List<Car> findByUserIdAndArchivedFalse(Long userId);
}
