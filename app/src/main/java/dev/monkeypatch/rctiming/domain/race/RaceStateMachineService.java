package dev.monkeypatch.rctiming.domain.race;

import dev.monkeypatch.rctiming.domain.event.IllegalStateTransitionException;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
public class RaceStateMachineService {

    private static final Map<RaceStatus, Set<RaceStatus>> VALID_TRANSITIONS;

    static {
        Map<RaceStatus, Set<RaceStatus>> m = new EnumMap<>(RaceStatus.class);
        m.put(RaceStatus.PENDING,  EnumSet.of(RaceStatus.GRID));
        m.put(RaceStatus.GRID,     EnumSet.of(RaceStatus.RUNNING, RaceStatus.PENDING));
        m.put(RaceStatus.RUNNING,  EnumSet.of(RaceStatus.STOPPED, RaceStatus.FINISHED));
        m.put(RaceStatus.STOPPED,  EnumSet.of(RaceStatus.RUNNING, RaceStatus.FINISHED));
        m.put(RaceStatus.FINISHED, EnumSet.noneOf(RaceStatus.class));
        VALID_TRANSITIONS = Map.copyOf(m);
    }

    public void transition(Race race, RaceStatus target) {
        Set<RaceStatus> valid = VALID_TRANSITIONS.getOrDefault(race.getStatus(), Set.of());
        if (!valid.contains(target)) {
            throw new IllegalStateTransitionException(
                "Cannot transition race " + race.getId()
                + " from " + race.getStatus() + " to " + target);
        }
        race.setStatus(target);
    }
}
