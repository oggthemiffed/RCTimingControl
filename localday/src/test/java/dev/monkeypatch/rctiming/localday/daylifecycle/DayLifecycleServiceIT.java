package dev.monkeypatch.rctiming.localday.daylifecycle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.monkeypatch.rctiming.localday.auth.LocalCredential;
import dev.monkeypatch.rctiming.localday.auth.LocalCredentialRepository;
import dev.monkeypatch.rctiming.localday.auth.LocalSession;
import dev.monkeypatch.rctiming.localday.auth.LocalSessionRepository;
import dev.monkeypatch.rctiming.localday.daylifecycle.dto.CloudLifecycleOpenResponse;
import dev.monkeypatch.rctiming.localday.daylifecycle.dto.CloudLoginResponse;
import dev.monkeypatch.rctiming.localday.daylifecycle.dto.CloudPreCacheCredentialDto;
import dev.monkeypatch.rctiming.localday.daylifecycle.dto.CloudPreCacheEntryDto;
import dev.monkeypatch.rctiming.localday.daylifecycle.dto.CloudPreCacheFormatConfigDto;
import dev.monkeypatch.rctiming.localday.daylifecycle.dto.CloudPreCacheInstanceSecretDto;
import dev.monkeypatch.rctiming.localday.daylifecycle.dto.CloudPreCacheResponse;
import dev.monkeypatch.rctiming.localday.daylifecycle.dto.CloudPreCacheScheduleDto;
import dev.monkeypatch.rctiming.localday.domain.CachedEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedEntryRepository;
import dev.monkeypatch.rctiming.localday.domain.CachedFormatConfig;
import dev.monkeypatch.rctiming.localday.domain.CachedFormatConfigRepository;
import dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntryRepository;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Full-context integration test for {@link DayLifecycleService} against the real embedded
 * Postgres store (mirrors {@code CachedRepositoryIT}'s convention), with {@link PreCacheClient}
 * replaced by a Mockito mock via {@code @MockitoBean} so no real HTTP call is ever made — the
 * cloud contract shapes themselves are covered separately by {@code PreCacheClientTest}.
 *
 * <p>All test methods share one embedded-Postgres-backed Spring context (no per-test rollback,
 * matching {@code CachedRepositoryIT}'s established convention) — each test uses its own
 * disjoint block of cloud ids (via {@link #preCacheResponse}'s {@code idBase} parameter) so tests
 * never read or overwrite another test's rows. The one exception is the final purge test, which
 * by its nature deletes every row in these tables regardless of which test created them —
 * {@code @TestMethodOrder} pins it to run last so it doesn't undermine the other tests' fixtures.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DayLifecycleServiceIT {

    @TempDir
    static Path dataDir;

    @DynamicPropertySource
    static void localdayProperties(DynamicPropertyRegistry registry) {
        registry.add("localday.datasource.embedded-postgres.data-directory",
                () -> dataDir.resolve("pg").toString());
    }

    @Autowired
    private DayLifecycleService dayLifecycleService;

    @Autowired
    private DayLifecycleStateRepository stateRepository;

    @Autowired
    private CachedEntryRepository cachedEntryRepository;

    @Autowired
    private CachedScheduleEntryRepository cachedScheduleEntryRepository;

    @Autowired
    private CachedFormatConfigRepository cachedFormatConfigRepository;

    @Autowired
    private LocalCredentialRepository localCredentialRepository;

    @Autowired
    private LocalSessionRepository localSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PreCacheClient preCacheClient;

    // Real SnapshotPushService would attempt a genuine (doomed, since nothing is listening)
    // HTTP call to the configured cloud base-url on every close() per this class's own fix —
    // mocked here the same way PreCacheClient is, so tests control and verify that interaction
    // directly instead of relying on it failing harmlessly.
    @MockitoBean
    private dev.monkeypatch.rctiming.localday.sync.SnapshotPushService snapshotPushService;

    private CloudLoginResponse loginResponse() {
        return new CloudLoginResponse("tok-1", "1", "official@club.test", "Race", "Director", List.of("ADMIN"));
    }

    /** {@code idBase} keeps each test's cloud ids in a disjoint block so tests never collide. */
    private CloudPreCacheResponse preCacheResponse(long idBase, String racerName, String officialName, String pin) throws Exception {
        JsonNode config = objectMapper.readTree("""
                {"type":"TIMED","durationSeconds":300}
                """);
        return new CloudPreCacheResponse(
                List.of(new CloudPreCacheEntryDto(idBase, "TX-" + idBase, racerName, "Car A", "Stock")),
                List.of(new CloudPreCacheScheduleDto(idBase + 1, 1, 1, 1, "Stock", null, "PENDING")),
                List.of(new CloudPreCacheFormatConfigDto(idBase + 2, "Stock", config)),
                List.of(new CloudPreCacheCredentialDto(idBase + 3, officialName, pin)),
                new CloudPreCacheInstanceSecretDto("ignored", "super-secret-" + idBase));
    }

    // --- Happy path: pre-cache caches all four categories and mints usable credentials ---

    @Test
    @Order(1)
    void preCache_withConnectivity_cachesAllFourCategoriesAndMintsCredentials() throws Exception {
        long idBase = 1_000L;
        when(preCacheClient.login("official@club.test", "hunter2")).thenReturn(loginResponse());
        when(preCacheClient.preCache(eq(10L), eq("tok-1"), anyString()))
                .thenReturn(preCacheResponse(idBase, "Ada Lovelace", "Jane Director", "1234"));

        DayLifecycleState state = dayLifecycleService.preCache(10L, "official@club.test", "hunter2");

        assertThat(state.getStatus()).isEqualTo(DayLifecycleStatus.PRE_CACHED);
        assertThat(state.getCloudEventId()).isEqualTo(10L);
        assertThat(state.getInstanceSecret()).isEqualTo("super-secret-" + idBase);
        assertThat(state.getLastPreCachedAt()).isNotNull();

        CachedEntry entry = cachedEntryRepository.findByCloudEntryId(idBase).orElseThrow();
        assertThat(entry.getRacerName()).isEqualTo("Ada Lovelace");
        assertThat(entry.getTransponderNumber()).isEqualTo("TX-" + idBase);

        CachedScheduleEntry schedule = cachedScheduleEntryRepository.findByCloudRaceId(idBase + 1).orElseThrow();
        assertThat(schedule.getRoundNumber()).isEqualTo(1);

        CachedFormatConfig formatConfig = cachedFormatConfigRepository.findByCloudFormatId(idBase + 2).orElseThrow();
        assertThat(formatConfig.getConfig()).contains("\"TIMED\"");

        LocalCredential credential = localCredentialRepository.findByCloudUserId(idBase + 3).orElseThrow();
        assertThat(credential.getOfficialName()).isEqualTo("Jane Director");
        assertThat(passwordEncoder.matches("1234", credential.getSecretHash())).isTrue();
    }

    // --- Online open re-pulls and overwrites a stale local cache ---

    @Test
    @Order(2)
    void open_online_rePullsAndOverwritesStaleCache() throws Exception {
        long idBase = 2_000L;
        when(preCacheClient.login("official@club.test", "hunter2")).thenReturn(loginResponse());
        when(preCacheClient.preCache(eq(11L), eq("tok-1"), anyString()))
                .thenReturn(preCacheResponse(idBase, "Old Name", "Some Official", "1111"))
                .thenReturn(preCacheResponse(idBase, "Corrected Name", "Some Official", "1111"));
        when(preCacheClient.openLifecycle(eq(11L), eq("tok-1"), anyString()))
                .thenReturn(new CloudLifecycleOpenResponse(11L, true, null, null, 5L));

        // First pre-cache establishes the stale value.
        dayLifecycleService.preCache(11L, "official@club.test", "hunter2");
        assertThat(cachedEntryRepository.findByCloudEntryId(idBase).orElseThrow().getRacerName())
                .isEqualTo("Old Name");

        // open() re-pulls, and the second mocked response's value must win.
        DayLifecycleState state = dayLifecycleService.open(11L, "official@club.test", "hunter2");

        assertThat(state.getStatus()).isEqualTo(DayLifecycleStatus.OPEN);
        assertThat(state.getGeneration()).isEqualTo(5L);
        assertThat(state.isSplitBrainWarning()).isFalse();
        assertThat(cachedEntryRepository.findByCloudEntryId(idBase).orElseThrow().getRacerName())
                .isEqualTo("Corrected Name");
    }

    // --- Offline open with an existing PRE_CACHED state reuses the cache, no HTTP call ---

    @Test
    @Order(3)
    void open_offline_withExistingPreCachedState_reusesCacheAndSetsSplitBrainWarning() throws Exception {
        long idBase = 3_000L;
        when(preCacheClient.login("official@club.test", "hunter2")).thenReturn(loginResponse());
        when(preCacheClient.preCache(eq(12L), eq("tok-1"), anyString()))
                .thenReturn(preCacheResponse(idBase, "Someone", "Some Official", "2222"));

        dayLifecycleService.preCache(12L, "official@club.test", "hunter2");

        DayLifecycleState state = dayLifecycleService.open(12L, null, null);

        assertThat(state.getStatus()).isEqualTo(DayLifecycleStatus.OPEN);
        assertThat(state.isSplitBrainWarning()).isTrue();
        verify(preCacheClient, never()).openLifecycle(anyLong(), anyString(), anyString());
        // Only the one preCache() call above — the offline open must not trigger another.
        verify(preCacheClient, times(1)).preCache(eq(12L), anyString(), anyString());
    }

    // --- Offline open with NOT_SET_UP fails clearly ---

    @Test
    @Order(4)
    void open_offline_withNotSetUpState_throwsClearError() {
        assertThatThrownBy(() -> dayLifecycleService.open(999_999L, "", ""))
                .isInstanceOf(OfflineOpenUnavailableException.class);
        assertThatThrownBy(() -> dayLifecycleService.open(999_999L, null, null))
                .isInstanceOf(OfflineOpenUnavailableException.class);
    }

    // --- Online open falls back to offline path if the cloud becomes unreachable mid-attempt ---

    @Test
    @Order(5)
    void open_online_fallsBackToOfflineWhenCloudBecomesUnreachablePartway() throws Exception {
        long idBase = 4_000L;
        when(preCacheClient.login("official@club.test", "hunter2")).thenReturn(loginResponse());
        when(preCacheClient.preCache(eq(13L), eq("tok-1"), anyString()))
                .thenReturn(preCacheResponse(idBase, "Cached Before Drop", "Some Official", "3333"));
        dayLifecycleService.preCache(13L, "official@club.test", "hunter2");

        when(preCacheClient.login("official@club.test", "hunter2"))
                .thenThrow(new CloudUnreachableException("simulated drop", new java.net.ConnectException()));

        DayLifecycleState state = dayLifecycleService.open(13L, "official@club.test", "hunter2");

        assertThat(state.getStatus()).isEqualTo(DayLifecycleStatus.OPEN);
        assertThat(state.isSplitBrainWarning()).isTrue();
    }

    // --- close(): pendingSyncCount > 0 returns pending, no purge, no status change ---

    @Test
    @Order(6)
    void close_withPendingSyncCount_returnsPendingAndDoesNotPurgeOrChangeStatus() throws Exception {
        long idBase = 5_000L;
        when(preCacheClient.login("official@club.test", "hunter2")).thenReturn(loginResponse());
        when(preCacheClient.preCache(eq(14L), eq("tok-1"), anyString()))
                .thenReturn(preCacheResponse(idBase, "Pending Case", "Some Official", "4444"));
        dayLifecycleService.preCache(14L, "official@club.test", "hunter2");

        DayLifecycleState state = stateRepository.findById(DayLifecycleState.SINGLETON_ID).orElseThrow();
        state.setPendingSyncCount(3);
        stateRepository.save(state);

        DayCloseOutcome outcome = dayLifecycleService.close(true);

        assertThat(outcome).isInstanceOf(DayCloseOutcome.Pending.class);
        assertThat(((DayCloseOutcome.Pending) outcome).pendingSyncCount()).isEqualTo(3);

        DayLifecycleState after = stateRepository.findById(DayLifecycleState.SINGLETON_ID).orElseThrow();
        assertThat(after.getStatus()).isNotEqualTo(DayLifecycleStatus.CLOSED);
        assertThat(cachedEntryRepository.findByCloudEntryId(idBase)).isPresent();

        // Reset for later tests in this shared context.
        state.setPendingSyncCount(0);
        stateRepository.save(state);
    }

    // --- U12: an online open resets a stale superseded flag from a prior event ---

    @Test
    @Order(7)
    void open_online_resetsStaleSupersededFlagFromAPriorEvent() throws Exception {
        long idBase = 7_000L;
        when(preCacheClient.login("official@club.test", "hunter2")).thenReturn(loginResponse());
        when(preCacheClient.preCache(eq(16L), eq("tok-1"), anyString()))
                .thenReturn(preCacheResponse(idBase, "Fresh Event", "Some Official", "6666"));
        when(preCacheClient.openLifecycle(eq(16L), eq("tok-1"), anyString()))
                .thenReturn(new CloudLifecycleOpenResponse(16L, true, null, null, 9L));

        // Simulate this instance having been superseded during an earlier, already-closed event.
        DayLifecycleState state = stateRepository.findById(DayLifecycleState.SINGLETON_ID).orElseThrow();
        state.setSuperseded(true);
        state.setSupersededAt(Instant.parse("2026-08-20T10:00:00Z"));
        stateRepository.save(state);

        DayLifecycleState after = dayLifecycleService.open(16L, "official@club.test", "hunter2");

        assertThat(after.getStatus()).isEqualTo(DayLifecycleStatus.OPEN);
        assertThat(after.isSuperseded()).isFalse();
        assertThat(after.getSupersededAt()).isNull();
    }

    // --- U12: close() is not blocked forever by a superseded instance's frozen pendingSyncCount ---

    @Test
    @Order(8)
    void close_supersededWithFrozenPendingSyncCount_closesAnyway() throws Exception {
        long idBase = 8_000L;
        when(preCacheClient.login("official@club.test", "hunter2")).thenReturn(loginResponse());
        when(preCacheClient.preCache(eq(17L), eq("tok-1"), anyString()))
                .thenReturn(preCacheResponse(idBase, "Superseded Case", "Some Official", "7777"));
        dayLifecycleService.preCache(17L, "official@club.test", "hunter2");

        // A superseded instance's pendingSyncCount freezes at whatever it was when the rejected
        // push last set it — nothing ever attempts another push to drain it back to zero.
        DayLifecycleState state = stateRepository.findById(DayLifecycleState.SINGLETON_ID).orElseThrow();
        state.setSuperseded(true);
        state.setSupersededAt(Instant.parse("2026-08-24T12:00:00Z"));
        state.setPendingSyncCount(5);
        stateRepository.save(state);

        DayCloseOutcome outcome = dayLifecycleService.close(true);

        assertThat(outcome).isInstanceOf(DayCloseOutcome.Closed.class);
        DayLifecycleState after = stateRepository.findById(DayLifecycleState.SINGLETON_ID).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(DayLifecycleStatus.CLOSED);
        assertThat(cachedEntryRepository.findByCloudEntryId(idBase)).isEmpty();
    }

    // --- close() flushes a synchronous push before evaluating the pending-sync gate ---

    @Test
    @Order(9)
    void close_flushesSnapshotPushService_beforeEvaluatingPendingSyncGate() throws Exception {
        long idBase = 9_000L;
        when(preCacheClient.login("official@club.test", "hunter2")).thenReturn(loginResponse());
        when(preCacheClient.preCache(eq(18L), eq("tok-1"), anyString()))
                .thenReturn(preCacheResponse(idBase, "Flush Case", "Some Official", "8888"));
        dayLifecycleService.preCache(18L, "official@club.test", "hunter2");

        // Simulate a race-state transition's async event-triggered push not having run yet:
        // pendingSyncCount is stale (nonzero) until pushNow() is actually invoked.
        DayLifecycleState state = stateRepository.findById(DayLifecycleState.SINGLETON_ID).orElseThrow();
        state.setPendingSyncCount(4);
        stateRepository.save(state);

        doAnswer(invocation -> {
            DayLifecycleState current = stateRepository.findById(DayLifecycleState.SINGLETON_ID).orElseThrow();
            current.setPendingSyncCount(0);
            stateRepository.save(current);
            return null;
        }).when(snapshotPushService).pushNow();

        DayCloseOutcome outcome = dayLifecycleService.close(true);

        verify(snapshotPushService).pushNow();
        assertThat(outcome).isInstanceOf(DayCloseOutcome.Closed.class);
    }

    // --- close(): pendingSyncCount == 0 purges everything and sets CLOSED ---
    // Runs last by design since it purges ALL local_credentials/local_sessions/cached_* rows,
    // including those created by earlier tests in this shared context — matching the unit's
    // actual purge semantics (close purges everything, not just what this test itself created).

    @Test
    @Order(10)
    void close_withNoPendingSync_purgesCachedDataAndSetsClosed() throws Exception {
        long idBase = 6_000L;
        when(preCacheClient.login("official@club.test", "hunter2")).thenReturn(loginResponse());
        when(preCacheClient.preCache(eq(15L), eq("tok-1"), anyString()))
                .thenReturn(preCacheResponse(idBase, "Purge Case", "Purge Official", "5555"));
        when(preCacheClient.closeLifecycle(eq(15L), any(), eq(true)))
                .thenThrow(new CloudRequestException(401, "unauthenticated best-effort call", null));

        dayLifecycleService.preCache(15L, "official@club.test", "hunter2");

        LocalCredential credential = localCredentialRepository.findByCloudUserId(idBase + 3).orElseThrow();
        LocalSession session = new LocalSession();
        session.setCredentialId(credential.getId());
        session.setOfficialName(credential.getOfficialName());
        session.setSessionToken("session-token-for-close-test");
        session.setIssuedAt(Instant.now());
        localSessionRepository.save(session);

        DayCloseOutcome outcome = dayLifecycleService.close(true);

        assertThat(outcome).isInstanceOf(DayCloseOutcome.Closed.class);

        DayLifecycleState after = stateRepository.findById(DayLifecycleState.SINGLETON_ID).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(DayLifecycleStatus.CLOSED);

        assertThat(cachedEntryRepository.findAll()).isEmpty();
        assertThat(cachedScheduleEntryRepository.findAll()).isEmpty();
        assertThat(cachedFormatConfigRepository.findAll()).isEmpty();
        assertThat(localCredentialRepository.findAll()).isEmpty();
        assertThat(localSessionRepository.findAll()).isEmpty();
    }
}
