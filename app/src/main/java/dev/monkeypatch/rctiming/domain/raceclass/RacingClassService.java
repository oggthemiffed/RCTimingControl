package dev.monkeypatch.rctiming.domain.raceclass;

import dev.monkeypatch.rctiming.api.admin.dto.CreateRacingClassRequest;
import dev.monkeypatch.rctiming.api.admin.dto.RacingClassDto;
import dev.monkeypatch.rctiming.domain.audit.Actor;
import dev.monkeypatch.rctiming.domain.audit.AuditService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class RacingClassService {

    private final RacingClassRepository racingClassRepository;
    private final AuditService audit;

    public RacingClassService(RacingClassRepository racingClassRepository, AuditService audit) {
        this.racingClassRepository = racingClassRepository;
        this.audit = audit;
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

    public RacingClassDto create(Actor actor, CreateRacingClassRequest request) {
        RacingClass racingClass = new RacingClass();
        racingClass.setName(request.name());
        racingClass.setDescription(request.description());
        Instant now = Instant.now();
        racingClass.setCreatedAt(now);
        racingClass.setUpdatedAt(now);
        RacingClass saved = racingClassRepository.save(racingClass);
        audit.entry(actor, "CLASS_CREATED").entity("racing_class", saved.getId())
                .summary("Added the class " + saved.getName())
                .after(values(saved)).record();
        return RacingClassDto.from(saved);
    }

    public RacingClassDto update(Actor actor, Long id, CreateRacingClassRequest request) {
        RacingClass racingClass = getRacingClassOrThrow(id);
        Map<String, Object> before = values(racingClass);
        racingClass.setName(request.name());
        racingClass.setDescription(request.description());
        racingClass.setUpdatedAt(Instant.now());
        RacingClass saved = racingClassRepository.save(racingClass);
        audit.entry(actor, "CLASS_UPDATED").entity("racing_class", id)
                .summary("Changed the class " + saved.getName())
                .before(before).after(values(saved)).record();
        return RacingClassDto.from(saved);
    }

    public void delete(Actor actor, Long id) {
        RacingClass racingClass = getRacingClassOrThrow(id);
        racingClassRepository.deleteById(id);
        audit.entry(actor, "CLASS_DELETED").entity("racing_class", id)
                .summary("Removed the class " + racingClass.getName())
                .before(values(racingClass)).record();
    }

    private static Map<String, Object> values(RacingClass c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", c.getName());
        m.put("description", c.getDescription());
        return m;
    }

    private RacingClass getRacingClassOrThrow(Long id) {
        return racingClassRepository.getOrThrow(id);
    }
}
