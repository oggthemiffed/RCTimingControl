package dev.monkeypatch.rctiming.domain.competitor;

import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
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
    void forUser_firstCall_createsCompetitorKeyedByUserId() {
        CompetitorRepository repo = mock(CompetitorRepository.class);
        UserRepository users = mock(UserRepository.class);
        User dave = user(7L, "Dave", "Racer");
        when(users.findByIdForUpdate(7L)).thenReturn(Optional.of(dave));
        when(repo.findByExternalSourceAndExternalId("RCTC_USER", "7")).thenReturn(Optional.empty());
        when(repo.save(any(Competitor.class))).thenAnswer(inv -> inv.getArgument(0));

        Competitor competitor = new CompetitorService(repo, users).forUser(7L);

        assertThat(competitor.getDisplayName()).isEqualTo("Dave Racer");
        assertThat(competitor.getExternalSource()).isEqualTo("RCTC_USER");
        assertThat(competitor.getExternalId()).isEqualTo("7");
    }

    @Test
    void forUser_existingCompetitor_isReturnedWithoutSaving() {
        CompetitorRepository repo = mock(CompetitorRepository.class);
        UserRepository users = mock(UserRepository.class);
        User dave = user(7L, "Dave", "Racer");
        when(users.findByIdForUpdate(7L)).thenReturn(Optional.of(dave));
        Competitor existing = new Competitor();
        existing.setId(42L);
        when(repo.findByExternalSourceAndExternalId("RCTC_USER", "7")).thenReturn(Optional.of(existing));

        Competitor competitor = new CompetitorService(repo, users).forUser(7L);

        assertThat(competitor.getId()).isEqualTo(42L);
        verify(repo, never()).save(any());
    }

    @Test
    void forUser_unknownUser_isNotFound() {
        UserRepository users = mock(UserRepository.class);
        when(users.findByIdForUpdate(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new CompetitorService(mock(CompetitorRepository.class), users).forUser(9L))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }

    private static User user(Long id, String first, String last) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getFirstName()).thenReturn(first);
        when(user.getLastName()).thenReturn(last);
        return user;
    }
}
