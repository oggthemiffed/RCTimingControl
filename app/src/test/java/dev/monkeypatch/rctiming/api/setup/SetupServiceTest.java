package dev.monkeypatch.rctiming.api.setup;

import dev.monkeypatch.rctiming.api.auth.AuthResponse;
import dev.monkeypatch.rctiming.api.setup.dto.BootstrapRequest;
import dev.monkeypatch.rctiming.api.setup.dto.SetupProgressDto;
import dev.monkeypatch.rctiming.domain.club.ClubProfile;
import dev.monkeypatch.rctiming.domain.club.ClubProfileRepository;
import dev.monkeypatch.rctiming.domain.format.RaceFormatTemplateRepository;
import dev.monkeypatch.rctiming.domain.track.TrackRepository;
import dev.monkeypatch.rctiming.domain.user.Role;
import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
import dev.monkeypatch.rctiming.domain.user.UserService;
import dev.monkeypatch.rctiming.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SetupServiceTest {

    @Mock
    private ClubProfileRepository clubProfileRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private TrackRepository trackRepository;
    @Mock
    private RaceFormatTemplateRepository raceFormatTemplateRepository;

    private SetupService setupService;

    @BeforeEach
    void setUp() {
        setupService = new SetupService(
                clubProfileRepository, userRepository, userService,
                jwtTokenService, trackRepository, raceFormatTemplateRepository);
    }

    @Test
    void bootstrap_throws_whenAnyUserExists() {
        when(userRepository.count()).thenReturn(1L);
        assertThrows(IllegalStateException.class,
                () -> setupService.bootstrap(new BootstrapRequest("First", "Last", "admin@test.com", "password123")));
    }

    @Test
    void bootstrap_assignsAdminRole_notRacerRole() {
        when(userRepository.count()).thenReturn(0L);
        User adminUser = new User();
        adminUser.setId(1L);
        adminUser.setEmail("admin@test.com");
        adminUser.setFirstName("First");
        adminUser.setLastName("Last");
        adminUser.setRoles(Set.of(Role.ADMIN));
        Instant now = Instant.now();
        adminUser.setCreatedAt(now);
        adminUser.setUpdatedAt(now);
        when(userService.createAdmin("admin@test.com", "password123", "First", "Last")).thenReturn(adminUser);
        when(jwtTokenService.generateAccessToken(adminUser)).thenReturn("test-token");

        AuthResponse response = setupService.bootstrap(
                new BootstrapRequest("First", "Last", "admin@test.com", "password123"));

        assertThat(response.roles()).contains("ADMIN");
        assertThat(response.roles()).doesNotContain("RACER");
    }

    @Test
    void getProgress_returnsAllFalse_onEmptyDb() {
        when(clubProfileRepository.count()).thenReturn(0L);
        when(trackRepository.count()).thenReturn(0L);
        when(raceFormatTemplateRepository.count()).thenReturn(0L);
        when(userRepository.findAll()).thenReturn(List.of());
        when(clubProfileRepository.findAll()).thenReturn(List.of());

        SetupProgressDto result = setupService.getProgress();

        assertThat(result.club()).isFalse();
        assertThat(result.track()).isFalse();
        assertThat(result.format()).isFalse();
        assertThat(result.staff()).isFalse();
        assertThat(result.decoder()).isFalse();
    }

    @Test
    void getProgress_returnsClubTrue_whenClubProfileSaved() {
        when(clubProfileRepository.count()).thenReturn(1L);
        when(trackRepository.count()).thenReturn(0L);
        when(raceFormatTemplateRepository.count()).thenReturn(0L);
        when(userRepository.findAll()).thenReturn(List.of());
        // Club profile without decoder fields — decoder=false
        ClubProfile profile = new ClubProfile();
        when(clubProfileRepository.findAll()).thenReturn(List.of(profile));

        SetupProgressDto result = setupService.getProgress();

        assertThat(result.club()).isTrue();
        assertThat(result.decoder()).isFalse();
    }
}
