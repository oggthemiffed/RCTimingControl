package dev.monkeypatch.rctiming.domain.raceclass;

import dev.monkeypatch.rctiming.domain.audit.Actor;
import dev.monkeypatch.rctiming.domain.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public List<RacingClass> findAll() {
        return racingClassRepository.findAll();
    }

    @Transactional(readOnly = true)
    public RacingClass findById(Long id) {
        return getRacingClassOrThrow(id);
    }

    public RacingClass create(Actor actor, String name, String description) {
        RacingClass racingClass = new RacingClass();
        racingClass.setName(name);
        racingClass.setDescription(description);
        RacingClass saved = racingClassRepository.save(racingClass);
        audit.entry(actor, "CLASS_CREATED").entity("racing_class", saved.getId())
                .summary("Added the class " + saved.getName())
                .after(values(saved)).record();
        return saved;
    }

    public RacingClass update(Actor actor, Long id, String name, String description) {
        RacingClass racingClass = getRacingClassOrThrow(id);
        Map<String, Object> before = values(racingClass);
        racingClass.setName(name);
        racingClass.setDescription(description);
        RacingClass saved = racingClassRepository.save(racingClass);
        audit.entry(actor, "CLASS_UPDATED").entity("racing_class", id)
                .summary("Changed the class " + saved.getName())
                .before(before).after(values(saved)).record();
        return saved;
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
