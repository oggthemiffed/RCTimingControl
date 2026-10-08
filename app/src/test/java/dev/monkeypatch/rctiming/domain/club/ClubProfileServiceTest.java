package dev.monkeypatch.rctiming.domain.club;

import dev.monkeypatch.rctiming.domain.audit.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClubProfileServiceTest {

    private final ClubProfileRepository repository = mock(ClubProfileRepository.class);
    private final ClubProfileService service = new ClubProfileService(repository,
            mock(GoverningBodyAffiliationRepository.class), mock(ApplicationEventPublisher.class),
            mock(AuditService.class));

    @Test
    void beforeAProfileExists_readingTheAudioSettingsGivesTheDefaultsAndCreatesNothing() {
        when(repository.findCurrent()).thenReturn(Optional.empty());

        assertThat(service.audioSettings()).usingRecursiveComparison().isEqualTo(ClubAudioSettings.defaults());
        assertThat(service.defaultVoiceId()).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    void aBlankVoice_meansThePiperDefault() {
        ClubProfile profile = new ClubProfile();
        profile.setDefaultVoiceId(" ");
        when(repository.findCurrent()).thenReturn(Optional.of(profile));

        assertThat(service.defaultVoiceId()).isEmpty();
        profile.setDefaultVoiceId("en_GB-jenny_dioco-medium");
        assertThat(service.defaultVoiceId()).contains("en_GB-jenny_dioco-medium");
    }
}
