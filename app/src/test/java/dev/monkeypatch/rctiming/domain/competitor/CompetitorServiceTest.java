package dev.monkeypatch.rctiming.domain.competitor;

import dev.monkeypatch.rctiming.domain.EntityNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompetitorServiceTest {

    @Test
    void createWalkIn_savesATrimmedNameWithNoExternalIdentity() {
        CompetitorRepository repo = mock(CompetitorRepository.class);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Competitor competitor = new CompetitorService(repo).createWalkIn("  Ada Lovelace ");

        assertThat(competitor.getDisplayName()).isEqualTo("Ada Lovelace");
        assertThat(competitor.getExternalSource()).isNull();
        assertThat(competitor.getExternalId()).isNull();
        assertThat(competitor.getCreatedAt()).isNotNull();
    }

    private static Competitor existing() {
        Competitor c = new Competitor();
        c.setId(7L);
        c.setDisplayName("Siobhan Keane");
        return c;
    }

    @Test
    void setSpokenName_savesATrimmedValue() {
        CompetitorRepository repo = mock(CompetitorRepository.class);
        when(repo.findById(7L)).thenReturn(Optional.of(existing()));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Competitor c = new CompetitorService(repo).setSpokenName(7L, "  Shiv-awn Keen ");

        assertThat(c.getSpokenName()).isEqualTo("Shiv-awn Keen");
        assertThat(c.speechName()).isEqualTo("Shiv-awn Keen");
        assertThat(c.getDisplayName()).isEqualTo("Siobhan Keane");
        assertThat(c.getUpdatedAt()).isNotNull();
    }

    @Test
    void setSpokenName_blankClearsItAndTheDisplayNameIsSpokenAgain() {
        CompetitorRepository repo = mock(CompetitorRepository.class);
        Competitor withSpoken = existing();
        withSpoken.setSpokenName("Shiv-awn Keen");
        when(repo.findById(7L)).thenReturn(Optional.of(withSpoken));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Competitor c = new CompetitorService(repo).setSpokenName(7L, "   ");

        assertThat(c.getSpokenName()).isNull();
        assertThat(c.speechName()).isEqualTo("Siobhan Keane");
    }

    @Test
    void setSpokenName_refusesTextOverTheLimit() {
        CompetitorRepository repo = mock(CompetitorRepository.class);

        assertThatThrownBy(() -> new CompetitorService(repo)
                .setSpokenName(7L, "x".repeat(CompetitorService.MAX_SPOKEN_NAME_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void setSpokenName_unknownCompetitorIsNotFound() {
        CompetitorRepository repo = mock(CompetitorRepository.class);
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new CompetitorService(repo).setSpokenName(99L, "x"))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
