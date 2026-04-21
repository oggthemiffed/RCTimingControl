package dev.monkeypatch.rctiming.domain.car;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarTagCategoryRepository extends JpaRepository<CarTagCategory, Long> {
    List<CarTagCategory> findAllByOrderBySortOrderAsc();
    List<CarTagCategory> findByArchivedFalseOrderBySortOrderAsc();
}
