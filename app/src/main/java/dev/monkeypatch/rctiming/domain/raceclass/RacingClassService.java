package dev.monkeypatch.rctiming.domain.raceclass;

import dev.monkeypatch.rctiming.api.admin.dto.CreateRacingClassRequest;
import dev.monkeypatch.rctiming.api.admin.dto.RacingClassDto;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class RacingClassService {

    private final RacingClassRepository racingClassRepository;

    public RacingClassService(RacingClassRepository racingClassRepository) {
        this.racingClassRepository = racingClassRepository;
    }

    @Transactional(readOnly = true)
    public List<RacingClassDto> findAll() {
        return racingClassRepository.findAll().stream()
                .map(RacingClassDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RacingClassDto findById(Long id) {
        return RacingClassDto.from(getRacingClassOrThrow(id));
    }

    public RacingClassDto create(CreateRacingClassRequest request) {
        RacingClass racingClass = new RacingClass();
        racingClass.setName(request.name());
        racingClass.setDescription(request.description());
        Instant now = Instant.now();
        racingClass.setCreatedAt(now);
        racingClass.setUpdatedAt(now);
        return RacingClassDto.from(racingClassRepository.save(racingClass));
    }

    public RacingClassDto update(Long id, CreateRacingClassRequest request) {
        RacingClass racingClass = getRacingClassOrThrow(id);
        racingClass.setName(request.name());
        racingClass.setDescription(request.description());
        racingClass.setUpdatedAt(Instant.now());
        return RacingClassDto.from(racingClassRepository.save(racingClass));
    }

    public void delete(Long id) {
        if (!racingClassRepository.existsById(id)) {
            throw new EntityNotFoundException("Racing class not found: " + id);
        }
        racingClassRepository.deleteById(id);
    }

    private RacingClass getRacingClassOrThrow(Long id) {
        return racingClassRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Racing class not found: " + id));
    }
}
