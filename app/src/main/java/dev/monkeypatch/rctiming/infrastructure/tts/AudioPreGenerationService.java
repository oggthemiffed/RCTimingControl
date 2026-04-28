package dev.monkeypatch.rctiming.infrastructure.tts;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pre-generates all predictable audio clips for a race when it transitions to {@code GRID} state.
 * <p>
 * Clips generated (AUDIO-09):
 * <ul>
 *   <li>Countdown intervals: 10m, 5m, 2m, 1m, 30s</li>
 *   <li>Car stagger calls: one per entry (using grid position as car number)</li>
 *   <li>Finish announcements: one per racer</li>
 * </ul>
 * All clip URLs are cached in-memory keyed by raceId and served to the race control client
 * via {@link #getClipMap(Long)} (AUDIO-10).
 */
@Service
public class AudioPreGenerationService {

    private static final Logger log = LoggerFactory.getLogger(AudioPreGenerationService.class);

    /** Countdown intervals in seconds: 10 min, 5 min, 2 min, 1 min, 30 sec */
    private static final int[] COUNTDOWN_SECONDS = {600, 300, 120, 60, 30};

    private final TtsClipService clipService;
    private final RaceRepository raceRepository;
    private final RaceEntryRepository raceEntryRepository;
    private final EntryRepository entryRepository;
    private final UserRepository userRepository;
    private final ClubProfileRepository clubProfileRepository;

    /** In-memory clip cache: raceId → Map<clipKey, url> */
    private final Map<Long, Map<String, String>> clipCache = new ConcurrentHashMap<>();

    public AudioPreGenerationService(TtsClipService clipService,
                                     RaceRepository raceRepository,
                                     RaceEntryRepository raceEntryRepository,
                                     EntryRepository entryRepository,
                                     UserRepository userRepository,
                                     ClubProfileRepository clubProfileRepository) {
        this.clipService = clipService;
        this.raceRepository = raceRepository;
        this.raceEntryRepository = raceEntryRepository;
        this.entryRepository = entryRepository;
        this.userRepository = userRepository;
        this.clubProfileRepository = clubProfileRepository;
    }

    // -------------------------------------------------------------------------
    // Event listener
    // -------------------------------------------------------------------------

    /**
     * React to race GRID transition and pre-generate all predictable clips.
     * Runs asynchronously so it does not block the race transition itself.
     */
    @Async
    @EventListener
    public void onRaceStatusChanged(RaceStatusChangedEvent event) {
        if (event.getNewStatus() != RaceStatus.GRID) {
            return;
        }

        Long raceId = event.getRaceId();
        log.info("Race {} entered GRID — starting audio pre-generation", raceId);

        Race race = raceRepository.findById(raceId).orElse(null);
        if (race == null) {
            log.warn("Race {} not found for audio pre-generation", raceId);
            return;
        }

        // Resolve club default voice
        String voiceId = clubProfileRepository.findAll().stream()
                .findFirst()
                .map(ClubProfile::getDefaultVoiceId)
                .orElse("en_GB-alan-medium");

        String raceName = "Race " + race.getHeatNumber();
        Map<String, String> clips = new HashMap<>();

        // 1. Countdown clips (AUDIO-02)
        for (int seconds : COUNTDOWN_SECONDS) {
            String label = formatCountdownLabel(seconds);
            String url = clipService.generateCountdownClip(raceId, seconds, raceName + ", " + label, voiceId);
            if (url != null) {
                clips.put("countdown-" + seconds, url);
            }
        }

        // 2. Stagger car-number calls (AUDIO-03)
        List<RaceEntry> entries = raceEntryRepository.findByRaceIdOrderByGridPosition(raceId);
        for (RaceEntry raceEntry : entries) {
            Integer gridPos = raceEntry.getGridPosition();
            if (gridPos == null) continue;
            String text = "Car " + gridPos + ", on the line";
            String url = clipService.generateCarNumberClip(raceId, gridPos, text, voiceId);
            if (url != null) {
                clips.put("car-" + gridPos, url);
            }
        }

        // 3. Finish announcements (AUDIO-05) — one per racer
        for (RaceEntry raceEntry : entries) {
            Entry entry = entryRepository.findById(raceEntry.getEntryId()).orElse(null);
            if (entry == null) continue;
            User user = userRepository.findById(entry.getUserId()).orElse(null);
            if (user == null) continue;

            Long racerId = user.getId();
            String racerName = user.getPhoneticName() != null && !user.getPhoneticName().isBlank()
                    ? user.getPhoneticName()
                    : user.getFirstName() + " " + user.getLastName();

            String url = clipService.generateFinishClip(raceId, racerId, racerName + " has finished", voiceId);
            if (url != null) {
                clips.put("finish-" + racerId, url);
            }
        }

        clipCache.put(raceId, Collections.unmodifiableMap(clips));
        log.info("Audio pre-generation complete for race {}: {} clips cached", raceId, clips.size());
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns pre-generated clip URL map for the given race. Empty map if not yet generated.
     * Called by {@code AudioClipController} (AUDIO-10).
     */
    public Map<String, String> getClipMap(Long raceId) {
        return clipCache.getOrDefault(raceId, Collections.emptyMap());
    }

    /**
     * Evicts cached clips for a finished race to free memory.
     */
    public void clearClips(Long raceId) {
        clipCache.remove(raceId);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String formatCountdownLabel(int seconds) {
        if (seconds >= 60 && seconds % 60 == 0) {
            int minutes = seconds / 60;
            return minutes + " minute" + (minutes == 1 ? "" : "s");
        }
        return seconds + " seconds";
    }
}
