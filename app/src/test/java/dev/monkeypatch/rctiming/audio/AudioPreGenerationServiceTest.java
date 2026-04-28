package dev.monkeypatch.rctiming.audio;

import dev.monkeypatch.rctiming.domain.club.ClubAudioSettings;
import dev.monkeypatch.rctiming.domain.club.ClubProfile;
import dev.monkeypatch.rctiming.domain.club.ClubProfileRepository;
import dev.monkeypatch.rctiming.domain.entry.Entry;
import dev.monkeypatch.rctiming.domain.entry.EntryRepository;
import dev.monkeypatch.rctiming.domain.race.Race;
import dev.monkeypatch.rctiming.domain.race.RaceEntry;
import dev.monkeypatch.rctiming.domain.race.RaceEntryRepository;
import dev.monkeypatch.rctiming.domain.race.RaceRepository;
import dev.monkeypatch.rctiming.domain.race.RaceStatus;
import dev.monkeypatch.rctiming.domain.race.RaceStatusChangedEvent;
import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
import dev.monkeypatch.rctiming.infrastructure.tts.AudioPreGenerationService;
import dev.monkeypatch.rctiming.infrastructure.tts.TtsClipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AudioPreGenerationServiceTest {

    @Mock TtsClipService clipService;
    @Mock RaceRepository raceRepository;
    @Mock RaceEntryRepository raceEntryRepository;
    @Mock EntryRepository entryRepository;
    @Mock UserRepository userRepository;
    @Mock ClubProfileRepository clubProfileRepository;

    @InjectMocks AudioPreGenerationService service;

    private Race race;
    private ClubProfile clubProfile;

    @BeforeEach
    void setUp() {
        race = new Race();
        race.setId(1L);
        race.setHeatNumber(3);

        clubProfile = new ClubProfile();
        clubProfile.setId(1L);
        clubProfile.setName("Test Club");
        clubProfile.setDefaultVoiceId("en_GB-alan-medium");
        clubProfile.setAudioSettings(ClubAudioSettings.defaults());
    }

    @Test
    void onRaceGridTransition_generatesCountdownClips() {
        when(raceRepository.findById(1L)).thenReturn(Optional.of(race));
        when(raceEntryRepository.findByRaceIdOrderByGridPosition(1L)).thenReturn(Collections.emptyList());
        when(clubProfileRepository.findAll()).thenReturn(List.of(clubProfile));
        when(clipService.generateCountdownClip(anyLong(), anyInt(), anyString(), anyString()))
                .thenReturn("http://minio/clip.wav");

        service.onRaceStatusChanged(new RaceStatusChangedEvent(this, 1L, RaceStatus.GRID));

        // 5 countdown intervals: 600, 300, 120, 60, 30
        verify(clipService, atLeast(5)).generateCountdownClip(eq(1L), anyInt(), anyString(), anyString());
        verify(clipService).generateCountdownClip(eq(1L), eq(600), anyString(), anyString());
        verify(clipService).generateCountdownClip(eq(1L), eq(30), anyString(), anyString());
    }

    @Test
    void onRaceGridTransition_generatesStaggerCallClips() {
        RaceEntry entry1 = new RaceEntry();
        entry1.setId(10L);
        entry1.setRaceId(1L);
        entry1.setEntryId(100L);
        entry1.setGridPosition(1);

        RaceEntry entry2 = new RaceEntry();
        entry2.setId(11L);
        entry2.setRaceId(1L);
        entry2.setEntryId(101L);
        entry2.setGridPosition(2);

        when(raceRepository.findById(1L)).thenReturn(Optional.of(race));
        when(raceEntryRepository.findByRaceIdOrderByGridPosition(1L)).thenReturn(List.of(entry1, entry2));
        when(clubProfileRepository.findAll()).thenReturn(List.of(clubProfile));
        when(entryRepository.findById(anyLong())).thenReturn(Optional.empty()); // no entry data needed for car clip
        when(clipService.generateCountdownClip(anyLong(), anyInt(), anyString(), anyString())).thenReturn(null);
        when(clipService.generateCarNumberClip(anyLong(), anyInt(), anyString(), anyString()))
                .thenReturn("http://minio/car.wav");

        service.onRaceStatusChanged(new RaceStatusChangedEvent(this, 1L, RaceStatus.GRID));

        verify(clipService).generateCarNumberClip(eq(1L), eq(1), anyString(), anyString());
        verify(clipService).generateCarNumberClip(eq(1L), eq(2), anyString(), anyString());
    }

    @Test
    void onRaceGridTransition_generatesFinishClips() {
        RaceEntry raceEntry = new RaceEntry();
        raceEntry.setId(10L);
        raceEntry.setRaceId(1L);
        raceEntry.setEntryId(100L);
        raceEntry.setGridPosition(1);

        Entry entry = new Entry();
        entry.setId(100L);
        entry.setUserId(200L);

        User user = new User();
        user.setId(200L);
        user.setFirstName("Alan");
        user.setLastName("Smith");

        when(raceRepository.findById(1L)).thenReturn(Optional.of(race));
        when(raceEntryRepository.findByRaceIdOrderByGridPosition(1L)).thenReturn(List.of(raceEntry));
        when(clubProfileRepository.findAll()).thenReturn(List.of(clubProfile));
        when(entryRepository.findById(100L)).thenReturn(Optional.of(entry));
        when(userRepository.findById(200L)).thenReturn(Optional.of(user));
        when(clipService.generateCountdownClip(anyLong(), anyInt(), anyString(), anyString())).thenReturn(null);
        when(clipService.generateCarNumberClip(anyLong(), anyInt(), anyString(), anyString())).thenReturn(null);
        when(clipService.generateFinishClip(anyLong(), anyLong(), anyString(), anyString()))
                .thenReturn("http://minio/finish.wav");

        service.onRaceStatusChanged(new RaceStatusChangedEvent(this, 1L, RaceStatus.GRID));

        verify(clipService).generateFinishClip(eq(1L), eq(200L), anyString(), anyString());
    }

    @Test
    void onRaceGridTransition_usesPhoneticNameWhenPresent() {
        RaceEntry raceEntry = new RaceEntry();
        raceEntry.setId(10L);
        raceEntry.setRaceId(1L);
        raceEntry.setEntryId(100L);
        raceEntry.setGridPosition(1);

        Entry entry = new Entry();
        entry.setId(100L);
        entry.setUserId(200L);

        User user = new User();
        user.setId(200L);
        user.setFirstName("Alan");
        user.setLastName("Smith");
        user.setPhoneticName("Ay-lan");

        when(raceRepository.findById(1L)).thenReturn(Optional.of(race));
        when(raceEntryRepository.findByRaceIdOrderByGridPosition(1L)).thenReturn(List.of(raceEntry));
        when(clubProfileRepository.findAll()).thenReturn(List.of(clubProfile));
        when(entryRepository.findById(100L)).thenReturn(Optional.of(entry));
        when(userRepository.findById(200L)).thenReturn(Optional.of(user));
        when(clipService.generateCountdownClip(anyLong(), anyInt(), anyString(), anyString())).thenReturn(null);
        when(clipService.generateCarNumberClip(anyLong(), anyInt(), anyString(), anyString())).thenReturn(null);
        when(clipService.generateFinishClip(anyLong(), anyLong(), anyString(), anyString())).thenReturn("http://x");

        service.onRaceStatusChanged(new RaceStatusChangedEvent(this, 1L, RaceStatus.GRID));

        verify(clipService).generateFinishClip(eq(1L), eq(200L), eq("Ay-lan has finished"), anyString());
    }

    @Test
    void getClipMap_returnsAllGeneratedUrls() {
        when(raceRepository.findById(1L)).thenReturn(Optional.of(race));
        when(raceEntryRepository.findByRaceIdOrderByGridPosition(1L)).thenReturn(Collections.emptyList());
        when(clubProfileRepository.findAll()).thenReturn(List.of(clubProfile));
        when(clipService.generateCountdownClip(anyLong(), anyInt(), anyString(), anyString()))
                .thenReturn("http://minio/clip.wav");

        service.onRaceStatusChanged(new RaceStatusChangedEvent(this, 1L, RaceStatus.GRID));

        Map<String, String> clips = service.getClipMap(1L);
        assertThat(clips).isNotEmpty();
        assertThat(clips).containsKey("countdown-600");
        assertThat(clips).containsKey("countdown-30");
    }

    @Test
    void getClipMap_returnsEmptyMapBeforeGeneration() {
        Map<String, String> clips = service.getClipMap(99L);
        assertThat(clips).isEmpty();
    }

    @Test
    void nonGridTransition_doesNotGenerateClips() {
        service.onRaceStatusChanged(new RaceStatusChangedEvent(this, 1L, RaceStatus.RUNNING));

        verify(clipService, never()).generateCountdownClip(anyLong(), anyInt(), anyString(), anyString());
        verify(clipService, never()).generateCarNumberClip(anyLong(), anyInt(), anyString(), anyString());
        verify(clipService, never()).generateFinishClip(anyLong(), anyLong(), anyString(), anyString());
    }

    @Test
    void raceNotFound_logsWarningAndDoesNotThrow() {
        when(raceRepository.findById(99L)).thenReturn(Optional.empty());

        // Should not throw
        service.onRaceStatusChanged(new RaceStatusChangedEvent(this, 99L, RaceStatus.GRID));

        verify(clipService, never()).generateCountdownClip(anyLong(), anyInt(), anyString(), anyString());
    }
}

