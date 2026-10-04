package dev.monkeypatch.rctiming.domain.competitor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
}
