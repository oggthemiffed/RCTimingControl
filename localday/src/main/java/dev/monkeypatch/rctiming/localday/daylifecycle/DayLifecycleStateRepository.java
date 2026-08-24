package dev.monkeypatch.rctiming.localday.daylifecycle;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DayLifecycleStateRepository extends JpaRepository<DayLifecycleState, Long> {
}
