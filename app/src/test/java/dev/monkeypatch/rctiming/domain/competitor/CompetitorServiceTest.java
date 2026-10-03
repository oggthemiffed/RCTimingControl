package dev.monkeypatch.rctiming.domain.competitor;

import dev.monkeypatch.rctiming.domain.user.User;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompetitorServiceTest {

    @Test
    void forUser_firstCall_createsCompetitorKeyedByUserId() {
        CompetitorRepository repo = mock(CompetitorRepository.class);
        when(repo.findByExternalSourceAndExternalId("RCTC_USER", "7")).thenReturn(Optional.empty());
        when(repo.save(any(Competitor.class))).thenAnswer(inv -> inv.getArgument(0));

        Competitor competitor = new CompetitorService(repo).forUser(user(7L, "Dave", "Racer"));

        assertThat(competitor.getDisplayName()).isEqualTo("Dave Racer");
        assertThat(competitor.getExternalSource()).isEqualTo("RCTC_USER");
        assertThat(competitor.getExternalId()).isEqualTo("7");
    }

    @Test
    void forUser_existingCompetitor_isReturnedWithoutSaving() {
        CompetitorRepository repo = mock(CompetitorRepository.class);
        Competitor existing = new Competitor();
        existing.setId(42L);
        when(repo.findByExternalSourceAndExternalId("RCTC_USER", "7")).thenReturn(Optional.of(existing));

        Competitor competitor = new CompetitorService(repo).forUser(user(7L, "Dave", "Racer"));

        assertThat(competitor.getId()).isEqualTo(42L);
        verify(repo, never()).save(any());
    }

    private static User user(Long id, String first, String last) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getFirstName()).thenReturn(first);
        when(user.getLastName()).thenReturn(last);
        return user;
    }
}
