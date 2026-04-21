package dev.monkeypatch.rctiming.domain.championship;

import dev.monkeypatch.rctiming.api.admin.dto.AddChampionshipClassRequest;
import dev.monkeypatch.rctiming.api.admin.dto.AddChampionshipEventRequest;
import dev.monkeypatch.rctiming.api.admin.dto.ChampionshipClassDto;
import dev.monkeypatch.rctiming.api.admin.dto.ChampionshipDetailDto;
import dev.monkeypatch.rctiming.api.admin.dto.ChampionshipDto;
import dev.monkeypatch.rctiming.api.admin.dto.ChampionshipEventLinkDto;
import dev.monkeypatch.rctiming.api.admin.dto.ChampionshipExclusionDto;
import dev.monkeypatch.rctiming.api.admin.dto.CreateChampionshipRequest;
import dev.monkeypatch.rctiming.api.admin.dto.CreateExclusionRequest;
import dev.monkeypatch.rctiming.api.admin.dto.PointsScaleEntryDto;
import dev.monkeypatch.rctiming.api.admin.dto.UpdateChampionshipRequest;
import dev.monkeypatch.rctiming.api.admin.dto.UpdatePointsScaleRequest;
import dev.monkeypatch.rctiming.domain.event.EventRepository;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClassRepository;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ChampionshipService {

    private final ChampionshipRepository championshipRepository;
    private final ChampionshipClassRepository classRepository;
    private final ChampionshipEventLinkRepository eventLinkRepository;
    private final ChampionshipPointsScaleRepository pointsScaleRepository;
    private final ChampionshipExclusionRepository exclusionRepository;
    private final EventRepository eventRepository;
    private final RacingClassRepository racingClassRepository;
    private final UserRepository userRepository;

    public ChampionshipService(ChampionshipRepository championshipRepository,
                               ChampionshipClassRepository classRepository,
                               ChampionshipEventLinkRepository eventLinkRepository,
                               ChampionshipPointsScaleRepository pointsScaleRepository,
                               ChampionshipExclusionRepository exclusionRepository,
                               EventRepository eventRepository,
                               RacingClassRepository racingClassRepository,
                               UserRepository userRepository) {
        this.championshipRepository = championshipRepository;
        this.classRepository = classRepository;
        this.eventLinkRepository = eventLinkRepository;
        this.pointsScaleRepository = pointsScaleRepository;
        this.exclusionRepository = exclusionRepository;
        this.eventRepository = eventRepository;
        this.racingClassRepository = racingClassRepository;
        this.userRepository = userRepository;
    }

    public ChampionshipDto create(CreateChampionshipRequest request) {
        Championship c = new Championship();
        c.setName(request.name());
        c.setBestXFromYX(request.bestXFromYX());
        c.setBestXFromYY(request.bestXFromYY());
        c.setScoringSource(request.scoringSource());
        c.setTqBonusPoints(request.tqBonusPoints());
        c.setAfinalWinnerBonusPoints(request.afinalWinnerBonusPoints());
        Instant now = Instant.now();
        c.setCreatedAt(now);
        c.setUpdatedAt(now);
        return ChampionshipDto.from(championshipRepository.save(c));
    }

    public ChampionshipDto update(Long id, UpdateChampionshipRequest request) {
        Championship c = getChampionshipOrThrow(id);
        c.setName(request.name());
        c.setBestXFromYX(request.bestXFromYX());
        c.setBestXFromYY(request.bestXFromYY());
        c.setScoringSource(request.scoringSource());
        c.setTqBonusPoints(request.tqBonusPoints());
        c.setAfinalWinnerBonusPoints(request.afinalWinnerBonusPoints());
        c.setUpdatedAt(Instant.now());
        return ChampionshipDto.from(championshipRepository.save(c));
    }

    @Transactional(readOnly = true)
    public List<ChampionshipDto> listAll() {
        return championshipRepository.findAll().stream()
                .map(ChampionshipDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChampionshipDetailDto getDetail(Long id) {
        Championship c = getChampionshipOrThrow(id);
        List<ChampionshipClassDto> classes = classRepository.findByChampionshipId(id)
                .stream().map(ChampionshipClassDto::from).toList();
        List<ChampionshipEventLinkDto> events = eventLinkRepository.findByChampionshipIdOrderByRoundNumberAsc(id)
                .stream().map(ChampionshipEventLinkDto::from).toList();
        List<PointsScaleEntryDto> scale = pointsScaleRepository.findByChampionshipIdOrderByPositionAsc(id)
                .stream().map(PointsScaleEntryDto::from).toList();
        return ChampionshipDetailDto.from(c, classes, events, scale);
    }

    public ChampionshipClassDto addClass(Long championshipId, AddChampionshipClassRequest request) {
        if (!championshipRepository.existsById(championshipId)) {
            throw new EntityNotFoundException("Championship not found: " + championshipId);
        }
        if (!racingClassRepository.existsById(request.racingClassId())) {
            throw new EntityNotFoundException("Racing class not found: " + request.racingClassId());
        }
        if (classRepository.existsByChampionshipIdAndRacingClassId(championshipId, request.racingClassId())) {
            throw new DataIntegrityViolationException(
                    "Racing class " + request.racingClassId() + " already belongs to championship " + championshipId);
        }
        ChampionshipClass cc = new ChampionshipClass();
        cc.setChampionshipId(championshipId);
        cc.setRacingClassId(request.racingClassId());
        cc.setBestXFromYX(request.bestXFromYX());
        cc.setBestXFromYY(request.bestXFromYY());
        cc.setCreatedAt(Instant.now());
        return ChampionshipClassDto.from(classRepository.save(cc));
    }

    public void removeClass(Long championshipId, Long racingClassId) {
        if (!championshipRepository.existsById(championshipId)) {
            throw new EntityNotFoundException("Championship not found: " + championshipId);
        }
        classRepository.deleteByChampionshipIdAndRacingClassId(championshipId, racingClassId);
    }

    public ChampionshipEventLinkDto linkEvent(Long championshipId, AddChampionshipEventRequest request) {
        if (!championshipRepository.existsById(championshipId)) {
            throw new EntityNotFoundException("Championship not found: " + championshipId);
        }
        if (!eventRepository.existsById(request.eventId())) {
            throw new EntityNotFoundException("Event not found: " + request.eventId());
        }
        if (eventLinkRepository.existsByChampionshipIdAndEventId(championshipId, request.eventId())) {
            throw new DataIntegrityViolationException(
                    "Event " + request.eventId() + " already linked to championship " + championshipId);
        }
        if (eventLinkRepository.existsByChampionshipIdAndRoundNumber(championshipId, request.roundNumber())) {
            throw new DataIntegrityViolationException(
                    "Round " + request.roundNumber() + " already assigned in championship " + championshipId);
        }
        ChampionshipEventLink link = new ChampionshipEventLink();
        link.setChampionshipId(championshipId);
        link.setEventId(request.eventId());
        link.setRoundNumber(request.roundNumber());
        link.setCreatedAt(Instant.now());
        return ChampionshipEventLinkDto.from(eventLinkRepository.save(link));
    }

    public void unlinkEvent(Long championshipId, Long eventId) {
        if (!championshipRepository.existsById(championshipId)) {
            throw new EntityNotFoundException("Championship not found: " + championshipId);
        }
        eventLinkRepository.deleteByChampionshipIdAndEventId(championshipId, eventId);
    }

    /** CHAMP-04: replace-all points scale in a single transaction. */
    public List<PointsScaleEntryDto> replacePointsScale(Long championshipId, UpdatePointsScaleRequest request) {
        if (!championshipRepository.existsById(championshipId)) {
            throw new EntityNotFoundException("Championship not found: " + championshipId);
        }
        pointsScaleRepository.deleteAllByChampionshipId(championshipId);
        List<ChampionshipPointsScaleEntry> toSave = new ArrayList<>(request.entries().size());
        for (PointsScaleEntryDto entry : request.entries()) {
            toSave.add(new ChampionshipPointsScaleEntry(championshipId, entry.position(), entry.points()));
        }
        List<ChampionshipPointsScaleEntry> saved = pointsScaleRepository.saveAll(toSave);
        return saved.stream()
                .sorted((a, b) -> Integer.compare(a.getPosition(), b.getPosition()))
                .map(PointsScaleEntryDto::from)
                .toList();
    }

    /**
     * CHAMP-02 + CHAMP-09: exclude a driver from one event and record an audit row.
     * `actingAdminId` is sourced from the JWT subject at the controller layer — NEVER from the request body.
     */
    public ChampionshipExclusionDto createExclusion(Long championshipId,
                                                    Long actingAdminId,
                                                    CreateExclusionRequest request) {
        if (!championshipRepository.existsById(championshipId)) {
            throw new EntityNotFoundException("Championship not found: " + championshipId);
        }
        if (!userRepository.existsById(request.driverId())) {
            throw new EntityNotFoundException("Driver user not found: " + request.driverId());
        }
        if (!eventRepository.existsById(request.eventId())) {
            throw new EntityNotFoundException("Event not found: " + request.eventId());
        }
        if (exclusionRepository.existsByChampionshipIdAndDriverIdAndEventId(
                championshipId, request.driverId(), request.eventId())) {
            throw new DataIntegrityViolationException(
                    "Driver " + request.driverId() + " is already excluded from event " + request.eventId()
                            + " in championship " + championshipId);
        }
        ChampionshipExclusion x = new ChampionshipExclusion();
        x.setChampionshipId(championshipId);
        x.setDriverId(request.driverId());
        x.setEventId(request.eventId());
        x.setReason(request.reason());
        x.setCreatedBy(actingAdminId);
        x.setCreatedAt(Instant.now());
        return ChampionshipExclusionDto.from(exclusionRepository.save(x));
    }

    public void deleteExclusion(Long championshipId, Long exclusionId) {
        ChampionshipExclusion x = exclusionRepository.findById(exclusionId)
                .orElseThrow(() -> new EntityNotFoundException("Exclusion not found: " + exclusionId));
        if (!x.getChampionshipId().equals(championshipId)) {
            throw new EntityNotFoundException(
                    "Exclusion " + exclusionId + " does not belong to championship " + championshipId);
        }
        exclusionRepository.delete(x);
    }

    @Transactional(readOnly = true)
    public List<ChampionshipExclusionDto> listExclusions(Long championshipId) {
        if (!championshipRepository.existsById(championshipId)) {
            throw new EntityNotFoundException("Championship not found: " + championshipId);
        }
        return exclusionRepository.findByChampionshipIdOrderByCreatedAtDesc(championshipId)
                .stream().map(ChampionshipExclusionDto::from).toList();
    }

    private Championship getChampionshipOrThrow(Long id) {
        return championshipRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Championship not found: " + id));
    }
}
