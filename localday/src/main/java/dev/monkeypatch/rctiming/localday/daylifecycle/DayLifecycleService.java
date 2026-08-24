package dev.monkeypatch.rctiming.localday.daylifecycle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.monkeypatch.rctiming.localday.auth.LocalCredential;
import dev.monkeypatch.rctiming.localday.auth.LocalCredentialRepository;
import dev.monkeypatch.rctiming.localday.auth.LocalSessionRepository;
import dev.monkeypatch.rctiming.localday.daylifecycle.dto.CloudLifecycleOpenResponse;
import dev.monkeypatch.rctiming.localday.daylifecycle.dto.CloudLoginResponse;
import dev.monkeypatch.rctiming.localday.daylifecycle.dto.CloudPreCacheCredentialDto;
import dev.monkeypatch.rctiming.localday.daylifecycle.dto.CloudPreCacheEntryDto;
import dev.monkeypatch.rctiming.localday.daylifecycle.dto.CloudPreCacheFormatConfigDto;
import dev.monkeypatch.rctiming.localday.daylifecycle.dto.CloudPreCacheResponse;
import dev.monkeypatch.rctiming.localday.daylifecycle.dto.CloudPreCacheScheduleDto;
import dev.monkeypatch.rctiming.localday.domain.CachedEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedEntryRepository;
import dev.monkeypatch.rctiming.localday.domain.CachedFormatConfig;
import dev.monkeypatch.rctiming.localday.domain.CachedFormatConfigRepository;
import dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntryRepository;
import dev.monkeypatch.rctiming.localday.race.RaceState;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates U10's pre-cache pull and day-open/day-close flow: the bootstrap problem of
 * getting a fresh copy of the cloud's entries/schedule/format-configs/official-credentials onto
 * this venue laptop before it goes offline for race day, plus the open/close lifecycle around
 * that pull (including the P0 split-brain mitigation — offline open is allowed, but flagged).
 *
 * <p>{@link PreCacheClient} is the only thing that talks to the cloud; everything here is local
 * orchestration and persistence via the domain module's repositories.
 */
@Service
public class DayLifecycleService {

    private final PreCacheClient preCacheClient;
    private final DayLifecycleStateRepository stateRepository;
    private final CachedEntryRepository cachedEntryRepository;
    private final CachedScheduleEntryRepository cachedScheduleEntryRepository;
    private final CachedFormatConfigRepository cachedFormatConfigRepository;
    private final LocalCredentialRepository localCredentialRepository;
    private final LocalSessionRepository localSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public DayLifecycleService(PreCacheClient preCacheClient,
                                DayLifecycleStateRepository stateRepository,
                                CachedEntryRepository cachedEntryRepository,
                                CachedScheduleEntryRepository cachedScheduleEntryRepository,
                                CachedFormatConfigRepository cachedFormatConfigRepository,
                                LocalCredentialRepository localCredentialRepository,
                                LocalSessionRepository localSessionRepository,
                                PasswordEncoder passwordEncoder,
                                ObjectMapper objectMapper) {
        this.preCacheClient = preCacheClient;
        this.stateRepository = stateRepository;
        this.cachedEntryRepository = cachedEntryRepository;
        this.cachedScheduleEntryRepository = cachedScheduleEntryRepository;
        this.cachedFormatConfigRepository = cachedFormatConfigRepository;
        this.localCredentialRepository = localCredentialRepository;
        this.localSessionRepository = localSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    /**
     * Logs in, pulls a fresh pre-cache payload for {@code eventId} using this instance's stable
     * {@code instanceId}, and upserts all four cacheable categories. A fresh pull always wins
     * while connected — existing local rows are overwritten with the cloud's current values, not
     * skipped, so a mid-day re-run picks up corrections made on the cloud side.
     *
     * <p>Does not downgrade {@code status} from {@code OPEN} back to {@code PRE_CACHED} — a
     * mid-day cache refresh while the day is already open should not look like the day was never
     * opened.
     */
    @Transactional
    public DayLifecycleState preCache(Long eventId, String email, String password) {
        CloudLoginResponse login = preCacheClient.login(email, password);
        DayLifecycleState state = getOrCreateState();
        return doPreCache(eventId, login.accessToken(), state);
    }

    /**
     * {@code email}/{@code password} blank or null means "attempt offline". Online attempts
     * always re-pre-cache first (see {@link #preCache}); if connectivity drops partway through
     * an online attempt, this falls back to the offline path rather than failing outright.
     */
    @Transactional
    public DayLifecycleState open(Long eventId, String email, String password) {
        if (StringUtils.hasText(email) && StringUtils.hasText(password)) {
            try {
                return openOnline(eventId, email, password);
            } catch (CloudUnreachableException e) {
                // Connectivity dropped between form-submit and request — fall through below.
            }
        }
        return openOffline(eventId);
    }

    private DayLifecycleState openOnline(Long eventId, String email, String password) {
        CloudLoginResponse login = preCacheClient.login(email, password);
        DayLifecycleState state = getOrCreateState();
        state = doPreCache(eventId, login.accessToken(), state);

        CloudLifecycleOpenResponse openResponse = preCacheClient.openLifecycle(eventId, login.accessToken(), state.getInstanceId());
        state.setCloudEventId(eventId);
        state.setGeneration(openResponse.generation());
        state.setStatus(DayLifecycleStatus.OPEN);
        state.setSplitBrainWarning(false);
        return stateRepository.save(state);
    }

    /**
     * v1's P0 split-brain mitigation is operator-warning-only — there is no new mutual-exclusion
     * mechanism here, just {@code splitBrainWarning} set true so the frontend can surface it.
     */
    private DayLifecycleState openOffline(Long eventId) {
        DayLifecycleState state = getOrCreateState();
        boolean hasUsableCache = state.getCloudEventId() != null
                && state.getCloudEventId().equals(eventId)
                && (state.getStatus() == DayLifecycleStatus.PRE_CACHED || state.getStatus() == DayLifecycleStatus.OPEN);
        if (!hasUsableCache) {
            throw new OfflineOpenUnavailableException(
                    "No earlier pre-cache/open found for event " + eventId + " on this instance; cannot open offline.");
        }
        state.setStatus(DayLifecycleStatus.OPEN);
        state.setSplitBrainWarning(true);
        // generation is intentionally left unchanged — it can't be incremented without the cloud.
        return stateRepository.save(state);
    }

    /**
     * AE2: a pending sync is reported, not silently dropped or falsely promoted to "closed" —
     * there is no real retry loop to build yet (nothing produces pending syncs until U11), this
     * just refuses to purge/close while one is outstanding.
     *
     * <p>{@code requestedSyncComplete} is deliberately not trusted on its own; the actual gate is
     * {@link DayLifecycleState#getPendingSyncCount()}.
     */
    @Transactional
    public DayCloseOutcome close(boolean requestedSyncComplete) {
        DayLifecycleState state = getOrCreateState();
        if (state.getPendingSyncCount() > 0) {
            return new DayCloseOutcome.Pending(state.getPendingSyncCount());
        }

        if (state.getCloudEventId() != null) {
            attemptBestEffortCloudClose(state);
        }

        // Purge: R14 requires pre-cached data to be deleted once the day closes and sync is
        // confirmed complete. local_sessions carries a FK to local_credentials, so it goes first.
        localSessionRepository.deleteAll();
        localCredentialRepository.deleteAll();
        cachedEntryRepository.deleteAll();
        cachedScheduleEntryRepository.deleteAll();
        cachedFormatConfigRepository.deleteAll();

        state.setStatus(DayLifecycleStatus.CLOSED);
        stateRepository.save(state);
        return new DayCloseOutcome.Closed();
    }

    /**
     * Best-effort — this local {@code /close} endpoint takes no credentials (an official is
     * already logged in locally, not necessarily freshly authenticated against the cloud), so
     * there is no access token available here to call the cloud's authenticated
     * {@code /lifecycle/close}. The call is still attempted unauthenticated on the chance it
     * reaches the cloud; any failure (unreachable, or the near-certain 401) is swallowed the same
     * way — the cloud's lock simply stays set until a later sync catches up, an accepted gap for
     * this unit, not a new one.
     */
    private void attemptBestEffortCloudClose(DayLifecycleState state) {
        try {
            preCacheClient.closeLifecycle(state.getCloudEventId(), null, true);
        } catch (RuntimeException e) {
            // Swallow — see javadoc above. Local close must proceed regardless.
        }
    }

    @Transactional(readOnly = true)
    public DayLifecycleState status() {
        return getOrCreateState();
    }

    private DayLifecycleState doPreCache(Long eventId, String accessToken, DayLifecycleState state) {
        CloudPreCacheResponse response = preCacheClient.preCache(eventId, accessToken, state.getInstanceId());

        for (CloudPreCacheEntryDto dto : emptyIfNull(response.entries())) {
            upsertEntry(dto);
        }
        for (CloudPreCacheScheduleDto dto : emptyIfNull(response.schedule())) {
            upsertScheduleEntry(dto);
        }
        for (CloudPreCacheFormatConfigDto dto : emptyIfNull(response.formatConfigs())) {
            upsertFormatConfig(dto);
        }
        for (CloudPreCacheCredentialDto dto : emptyIfNull(response.officialCredentials())) {
            upsertCredential(dto);
        }

        state.setCloudEventId(eventId);
        if (response.instanceSecret() != null) {
            state.setInstanceSecret(response.instanceSecret().secret());
        }
        state.setLastPreCachedAt(Instant.now());
        if (state.getStatus() != DayLifecycleStatus.OPEN) {
            state.setStatus(DayLifecycleStatus.PRE_CACHED);
        }
        return stateRepository.save(state);
    }

    private void upsertEntry(CloudPreCacheEntryDto dto) {
        CachedEntry entry = cachedEntryRepository.findByCloudEntryId(dto.cloudEntryId())
                .orElseGet(CachedEntry::new);
        entry.setCloudEntryId(dto.cloudEntryId());
        entry.setTransponderNumber(dto.transponderNumber());
        entry.setRacerName(dto.racerName());
        entry.setCarName(dto.carName());
        entry.setClassName(dto.className());
        entry.setSyncedAt(Instant.now());
        cachedEntryRepository.save(entry);
    }

    private void upsertScheduleEntry(CloudPreCacheScheduleDto dto) {
        CachedScheduleEntry schedule = cachedScheduleEntryRepository.findByCloudRaceId(dto.cloudRaceId())
                .orElseGet(CachedScheduleEntry::new);
        schedule.setCloudRaceId(dto.cloudRaceId());
        schedule.setRoundNumber(dto.roundNumber());
        schedule.setHeatNumber(dto.heatNumber());
        schedule.setSequence(dto.sequence());
        schedule.setClassName(dto.className());
        schedule.setFinalLetter(dto.finalLetter());
        if (StringUtils.hasText(dto.status())) {
            schedule.setStatus(RaceState.valueOf(dto.status()));
        }
        schedule.setSyncedAt(Instant.now());
        cachedScheduleEntryRepository.save(schedule);
    }

    private void upsertFormatConfig(CloudPreCacheFormatConfigDto dto) {
        CachedFormatConfig config = cachedFormatConfigRepository.findByCloudFormatId(dto.cloudEventClassId())
                .orElseGet(CachedFormatConfig::new);
        config.setCloudFormatId(dto.cloudEventClassId());
        config.setName(dto.className());
        config.setConfig(writeConfigJson(dto));
        config.setSyncedAt(Instant.now());
        cachedFormatConfigRepository.save(config);
    }

    private String writeConfigJson(CloudPreCacheFormatConfigDto dto) {
        try {
            return objectMapper.writeValueAsString(dto.config());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to re-serialize format config for cloudEventClassId="
                    + dto.cloudEventClassId(), e);
        }
    }

    private void upsertCredential(CloudPreCacheCredentialDto dto) {
        LocalCredential credential = localCredentialRepository.findByCloudUserId(dto.cloudUserId())
                .orElseGet(LocalCredential::new);
        credential.setCloudUserId(dto.cloudUserId());
        credential.setOfficialName(dto.officialName());
        credential.setSecretHash(passwordEncoder.encode(dto.pin()));
        credential.setRecovery(false);
        localCredentialRepository.save(credential);
    }

    /**
     * Lazily creates the singleton row on first use, minting {@link DayLifecycleState#getInstanceId()}
     * exactly once — it must stay stable for the lifetime of this venue-machine's database
     * (KTD4: generation-fencing on the cloud depends on a durable per-instance identity).
     */
    DayLifecycleState getOrCreateState() {
        return stateRepository.findById(DayLifecycleState.SINGLETON_ID)
                .orElseGet(() -> {
                    DayLifecycleState state = new DayLifecycleState();
                    state.setInstanceId(UUID.randomUUID().toString());
                    return stateRepository.save(state);
                });
    }

    private static <T> List<T> emptyIfNull(List<T> list) {
        return list == null ? List.of() : list;
    }
}
