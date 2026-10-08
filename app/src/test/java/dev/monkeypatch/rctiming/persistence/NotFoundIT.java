package dev.monkeypatch.rctiming.persistence;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import dev.monkeypatch.rctiming.domain.EntityNotFoundException;
import dev.monkeypatch.rctiming.domain.entry.EntryRepository;
import dev.monkeypatch.rctiming.domain.race.PenaltyRepository;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClassRepository;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
import dev.monkeypatch.rctiming.resultsexport.ResultsOutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A missing row is reported by the repository's {@code getOrThrow} and {@code requireExists}, named in the message. */
class NotFoundIT extends AbstractIntegrationTest {

    @Autowired RacingClassRepository racingClasses;
    @Autowired EntryRepository entries;
    @Autowired PenaltyRepository penalties;
    @Autowired UserRepository users;
    @Autowired ResultsOutboxRepository outbox;

    @Test
    void theMessageNamesWhatWasNotFound() {
        assertThatThrownBy(() -> racingClasses.getOrThrow(-1L))
                .isInstanceOf(EntityNotFoundException.class).hasMessage("Racing class not found: -1");
        assertThatThrownBy(() -> entries.getOrThrow(-1L)).hasMessage("Entry not found: -1");
        assertThatThrownBy(() -> penalties.getOrThrow(-1L)).hasMessage("Penalty not found: -1");
        assertThatThrownBy(() -> users.getOrThrow(-1L)).hasMessage("Official not found: -1");
        assertThatThrownBy(() -> outbox.getOrThrow(-1L)).hasMessage("Results export not found: -1");
    }

    @Test
    void requireExistsThrowsForAMissingRow() {
        assertThatThrownBy(() -> entries.requireExists(-1L))
                .isInstanceOf(EntityNotFoundException.class).hasMessage("Entry not found: -1");
    }
}
