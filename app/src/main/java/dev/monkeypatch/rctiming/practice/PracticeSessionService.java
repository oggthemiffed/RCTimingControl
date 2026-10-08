package dev.monkeypatch.rctiming.practice;

import dev.monkeypatch.rctiming.domain.EntityNotFoundException;
import dev.monkeypatch.rctiming.domain.StateConflictException;
import dev.monkeypatch.rctiming.domain.event.Event;
import dev.monkeypatch.rctiming.domain.event.EventRepository;
import dev.monkeypatch.rctiming.domain.practice.PracticeSession;
import dev.monkeypatch.rctiming.domain.practice.PracticeSessionRepository;
import dev.monkeypatch.rctiming.domain.practice.PracticeStatus;
import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
import dev.monkeypatch.rctiming.practice.dto.PracticeSessionDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * CRUD and state-machine service for practice sessions.
 * Delegates to PracticeTimingService for live timing start/stop lifecycle.
 */
@Service
public class PracticeSessionService {

    private final PracticeSessionRepository sessionRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final PracticeTimingService timingService;

    public PracticeSessionService(PracticeSessionRepository sessionRepository,
                                  EventRepository eventRepository,
                                  UserRepository userRepository,
                                  PracticeTimingService timingService) {
        this.sessionRepository = sessionRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.timingService = timingService;
    }

    // ---------------------------------------------------------------------------
    // CRUD
    // ---------------------------------------------------------------------------

    @Transactional
    public PracticeSessionDto create(CreateRequest request, String createdByEmail) {
        PracticeSession session = new PracticeSession();
        session.setName(request.name());

        if (request.eventId() != null) {
            Event event = eventRepository.findById(request.eventId())
                    .orElseThrow(() -> new IllegalArgumentException("Event not found: " + request.eventId()));
            session.setEventId(event.getId());
        }

        if (request.bestLapN() != null) {
            session.setBestLapN(request.bestLapN());
        }

        if (createdByEmail != null) {
            session.setCreatedByUserId(userRepository.findByEmail(createdByEmail).map(User::getId).orElse(null));
        }

        session = sessionRepository.save(session);
        return toDto(session);
    }

    public Optional<PracticeSessionDto> findById(Long id) {
        return sessionRepository.findById(id).map(this::toDto);
    }

    public List<PracticeSessionDto> findRecent(int limit) {
        return sessionRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(limit)
                .map(this::toDto)
                .toList();
    }

    // ---------------------------------------------------------------------------
    // State machine
    // ---------------------------------------------------------------------------

    @Transactional
    public PracticeSessionDto start(Long sessionId) {
        PracticeSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Session not found: " + sessionId));

        if (session.getStatus() != PracticeStatus.IDLE) {
            throw new StateConflictException("Session must be IDLE to start; current: " + session.getStatus());
        }

        session.start();
        session = sessionRepository.save(session);
        timingService.startSession(session);
        return toDto(session);
    }

    @Transactional
    public PracticeSessionDto stop(Long sessionId) {
        PracticeSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Session not found: " + sessionId));

        if (session.getStatus() != PracticeStatus.RUNNING) {
            throw new StateConflictException("Session must be RUNNING to stop; current: " + session.getStatus());
        }

        session.stop();
        session = sessionRepository.save(session);
        timingService.stopSession(sessionId);
        return toDto(session);
    }

    // ---------------------------------------------------------------------------
    // DTO mapping
    // ---------------------------------------------------------------------------

    private PracticeSessionDto toDto(PracticeSession session) {
        return new PracticeSessionDto(
                session.getId(),
                session.getName(),
                session.getEventId(),
                session.getEventId() != null
                        ? eventRepository.findById(session.getEventId()).map(Event::getName).orElse(null)
                        : null,
                session.getStatus(),
                session.getBestLapN(),
                session.getStartedAt(),
                session.getStoppedAt()
        );
    }

    // ---------------------------------------------------------------------------
    // Request record
    // ---------------------------------------------------------------------------

    public record CreateRequest(String name, Long eventId, Integer bestLapN) {}
}
