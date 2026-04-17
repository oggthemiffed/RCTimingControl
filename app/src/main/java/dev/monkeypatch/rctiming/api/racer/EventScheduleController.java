package dev.monkeypatch.rctiming.api.racer;

import dev.monkeypatch.rctiming.query.event.EventScheduleDto;
import dev.monkeypatch.rctiming.query.event.EventScheduleQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
public class EventScheduleController {

    private final EventScheduleQuery query;

    public EventScheduleController(EventScheduleQuery query) {
        this.query = query;
    }

    @GetMapping
    public List<EventScheduleDto> list() {
        return query.getPublicSchedule();
    }
}
