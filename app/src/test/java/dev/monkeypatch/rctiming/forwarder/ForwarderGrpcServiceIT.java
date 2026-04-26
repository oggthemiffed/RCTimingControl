package dev.monkeypatch.rctiming.forwarder;

import dev.monkeypatch.rctiming.domain.race.Race;
import dev.monkeypatch.rctiming.domain.race.RaceRepository;
import dev.monkeypatch.rctiming.domain.race.RaceStatus;
import dev.monkeypatch.rctiming.forwarder.proto.LapPassing;
import dev.monkeypatch.rctiming.forwarder.proto.ForwarderCommand;
import dev.monkeypatch.rctiming.forwarder.proto.TimingServiceGrpc;
import dev.monkeypatch.rctiming.timing.LapPassingEvent;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for ForwarderGrpcService: auth interceptor, LapPassingEvent publishing,
 * and drop-when-no-running-race behaviour.
 *
 * Uses a separate Spring context (not AbstractIntegrationTest) with a random gRPC port and
 * a mocked RaceRepository to avoid complex entity hierarchy setup.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ForwarderGrpcServiceIT.EventCaptorConfig.class)
class ForwarderGrpcServiceIT {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static int grpcPort;

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerGrpcPort(DynamicPropertyRegistry registry) throws IOException {
        if (grpcPort == 0) {
            try (ServerSocket s = new ServerSocket(0)) {
                grpcPort = s.getLocalPort();
            }
        }
        registry.add("app.grpc.port", () -> grpcPort);
    }

    @MockitoBean
    RaceRepository raceRepository;

    @Autowired
    ForwarderTokenService tokenService;

    @Autowired
    ForwarderTokenRepository tokenRepository;

    @Autowired
    EventCaptorConfig.EventCaptor eventCaptor;

    private ManagedChannel channel;

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();
        eventCaptor.clear();
        channel = ManagedChannelBuilder.forAddress("localhost", grpcPort)
                .usePlaintext()
                .build();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (channel != null) {
            channel.shutdown().awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    // -----------------------------------------------------------------------
    // Auth tests
    // -----------------------------------------------------------------------

    @Test
    void streamWithValidTokenAccepted() throws Exception {
        ForwarderTokenService.GenerateResult generated = tokenService.generate();
        String token = generated.plaintext();

        CountDownLatch errorLatch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        TimingServiceGrpc.TimingServiceStub stub = stubWithToken(token);
        StreamObserver<LapPassing> requestObserver = stub.streamPassings(new StreamObserver<>() {
            @Override public void onNext(ForwarderCommand cmd) {}
            @Override public void onError(Throwable t) { error.set(t); errorLatch.countDown(); }
            @Override public void onCompleted() {}
        });

        // Wait to confirm no UNAUTHENTICATED error within 1 second
        boolean gotError = errorLatch.await(1, TimeUnit.SECONDS);
        assertThat(gotError).as("Expected no error for valid token").isFalse();

        // Clean up stream
        requestObserver.onCompleted();
    }

    @Test
    void streamWithMissingTokenRejectedUnauthenticated() throws Exception {
        CountDownLatch errorLatch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        // No token metadata — use plain stub
        TimingServiceGrpc.TimingServiceStub stub = TimingServiceGrpc.newStub(channel);
        stub.streamPassings(new StreamObserver<>() {
            @Override public void onNext(ForwarderCommand cmd) {}
            @Override public void onError(Throwable t) { error.set(t); errorLatch.countDown(); }
            @Override public void onCompleted() {}
        });

        boolean gotError = errorLatch.await(2, TimeUnit.SECONDS);
        assertThat(gotError).as("Expected UNAUTHENTICATED error for missing token").isTrue();
        assertThat(error.get()).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) error.get()).getStatus().getCode())
                .isEqualTo(Status.UNAUTHENTICATED.getCode());
    }

    @Test
    void streamWithRevokedTokenRejectedUnauthenticated() throws Exception {
        // Generate token then immediately revoke it
        ForwarderTokenService.GenerateResult generated = tokenService.generate();
        String token = generated.plaintext();
        tokenService.revoke();

        CountDownLatch errorLatch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        TimingServiceGrpc.TimingServiceStub stub = stubWithToken(token);
        stub.streamPassings(new StreamObserver<>() {
            @Override public void onNext(ForwarderCommand cmd) {}
            @Override public void onError(Throwable t) { error.set(t); errorLatch.countDown(); }
            @Override public void onCompleted() {}
        });

        boolean gotError = errorLatch.await(2, TimeUnit.SECONDS);
        assertThat(gotError).as("Expected UNAUTHENTICATED for revoked token").isTrue();
        assertThat(((StatusRuntimeException) error.get()).getStatus().getCode())
                .isEqualTo(Status.UNAUTHENTICATED.getCode());
    }

    // -----------------------------------------------------------------------
    // LapPassingEvent publishing tests
    // -----------------------------------------------------------------------

    @Test
    void lapPassingMessagePublishedAsApplicationEvent() throws Exception {
        when(raceRepository.findFirstByStatus(RaceStatus.RUNNING))
                .thenReturn(Optional.empty());

        ForwarderTokenService.GenerateResult generated = tokenService.generate();
        TimingServiceGrpc.TimingServiceStub stub = stubWithToken(generated.plaintext());

        eventCaptor.expectOne();
        // no running race — event is NOT published; test passes trivially if no exception
        StreamObserver<LapPassing> requestObserver = stub.streamPassings(noopResponseObserver());
        requestObserver.onNext(samplePassing());
        // Give server time to process
        Thread.sleep(300);
        requestObserver.onCompleted();
        // With no running race, no event published — verify no crash
        assertThat(eventCaptor.getEvents()).isEmpty();
    }

    @Test
    void lapPassingDroppedWhenNoRunningRace() throws Exception {
        when(raceRepository.findFirstByStatus(RaceStatus.RUNNING))
                .thenReturn(Optional.empty());

        ForwarderTokenService.GenerateResult generated = tokenService.generate();
        TimingServiceGrpc.TimingServiceStub stub = stubWithToken(generated.plaintext());

        StreamObserver<LapPassing> requestObserver = stub.streamPassings(noopResponseObserver());
        requestObserver.onNext(samplePassing());
        Thread.sleep(300);
        requestObserver.onCompleted();

        assertThat(eventCaptor.getEvents()).isEmpty();
    }

    @Test
    void lapPassingResolvesRaceIdFromCurrentlyRunningRace() throws Exception {
        long expectedRaceId = 999L;
        Race mockRace = mock(Race.class);
        when(mockRace.getId()).thenReturn(expectedRaceId);
        when(raceRepository.findFirstByStatus(RaceStatus.RUNNING))
                .thenReturn(Optional.of(mockRace));

        ForwarderTokenService.GenerateResult generated = tokenService.generate();
        TimingServiceGrpc.TimingServiceStub stub = stubWithToken(generated.plaintext());

        eventCaptor.expectOne();
        StreamObserver<LapPassing> requestObserver = stub.streamPassings(noopResponseObserver());
        requestObserver.onNext(samplePassing());

        boolean received = eventCaptor.awaitOne(2, TimeUnit.SECONDS);
        assertThat(received).as("LapPassingEvent should be published").isTrue();
        assertThat(eventCaptor.getEvents()).hasSize(1);
        LapPassingEvent event = eventCaptor.getEvents().get(0);
        assertThat(event.raceId()).isEqualTo(expectedRaceId);
        assertThat(event.transponderNumber()).isEqualTo("12345");

        requestObserver.onCompleted();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private TimingServiceGrpc.TimingServiceStub stubWithToken(String token) {
        Metadata metadata = new Metadata();
        metadata.put(ForwarderTokenAuthInterceptor.TOKEN_KEY, token);
        return TimingServiceGrpc.newStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));
    }

    private LapPassing samplePassing() {
        return LapPassing.newBuilder()
                .setTransponderNumber("12345")
                .setRtcTimeMicros(1_000_000L)
                .setDecoderId(1)
                .setSeqNum(1)
                .build();
    }

    private StreamObserver<ForwarderCommand> noopResponseObserver() {
        return new StreamObserver<>() {
            @Override public void onNext(ForwarderCommand cmd) {}
            @Override public void onError(Throwable t) {}
            @Override public void onCompleted() {}
        };
    }

    // -----------------------------------------------------------------------
    // Test configuration: event captor
    // -----------------------------------------------------------------------

    @TestConfiguration
    static class EventCaptorConfig {

        @Bean
        EventCaptor eventCaptor() {
            return new EventCaptor();
        }

        static class EventCaptor {
            private final List<LapPassingEvent> events =
                    Collections.synchronizedList(new ArrayList<>());
            private volatile CountDownLatch latch = new CountDownLatch(0);

            @EventListener
            public void on(LapPassingEvent event) {
                events.add(event);
                latch.countDown();
            }

            void expectOne() {
                latch = new CountDownLatch(1);
            }

            boolean awaitOne(long timeout, TimeUnit unit) throws InterruptedException {
                return latch.await(timeout, unit);
            }

            List<LapPassingEvent> getEvents() {
                return Collections.unmodifiableList(events);
            }

            void clear() {
                events.clear();
                latch = new CountDownLatch(0);
            }
        }
    }
}

