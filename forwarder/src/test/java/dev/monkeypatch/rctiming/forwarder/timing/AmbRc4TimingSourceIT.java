package dev.monkeypatch.rctiming.forwarder.timing;

import dev.monkeypatch.rctiming.forwarder.simulator.FakeDecoderServer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** Integration test — starts FakeDecoderServer in generative mode. */
@Tag("integration")
class AmbRc4TimingSourceIT {

    /** Find a free port on loopback. */
    private static int freePort() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    @Test
    @Timeout(15)
    void connectsToLoopbackSimulator() throws Exception {
        int port = freePort();
        FakeDecoderServer server = FakeDecoderServer.generative(port,
                List.of("11111", "22222"), 200);
        server.start();

        List<EpochCorrectedPassing> received = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        AmbRc4TimingSource source = new AmbRc4TimingSource(
            "localhost", port,
            p -> { received.add(p); if (received.size() >= 1) latch.countDown(); },
            state -> {}
        );
        source.start();

        boolean got = latch.await(10, TimeUnit.SECONDS);
        source.stop();
        server.stop();

        assertThat(got).isTrue();
        assertThat(received).isNotEmpty();
    }

    @Test
    @Timeout(20)
    void reconnectsAfterStatusAbsenceExceedsThreshold() throws Exception {
        // Start server, connect, then stop server — verify RECONNECTING state fires
        int port = freePort();
        FakeDecoderServer server = FakeDecoderServer.generative(port,
                List.of("11111"), 300);
        server.start();

        CountDownLatch connectedLatch  = new CountDownLatch(1);
        CountDownLatch reconnectLatch  = new CountDownLatch(1);

        AmbRc4TimingSource source = new AmbRc4TimingSource(
            "localhost", port,
            p -> {},
            state -> {
                if (state == AmbRc4TimingSource.ConnectionState.CONNECTED) connectedLatch.countDown();
                if (state == AmbRc4TimingSource.ConnectionState.RECONNECTING) reconnectLatch.countDown();
            }
        );
        source.start();

        // Wait for initial connection
        assertThat(connectedLatch.await(5, TimeUnit.SECONDS)).isTrue();

        // Brief pause to ensure the accept loop has added the client socket to activeClients
        // (avoids race condition between CONNECTED callback and server.stop() closing clients)
        Thread.sleep(200);

        // Stop the server — TCP close → RECONNECTING
        server.stop();

        assertThat(reconnectLatch.await(8, TimeUnit.SECONDS)).isTrue();
        source.stop();
    }

    @Test
    @Timeout(15)
    void publishesParsedPassingsToCallback() throws Exception {
        int port = freePort();
        FakeDecoderServer server = FakeDecoderServer.generative(port,
                List.of("11111", "22222", "33333"), 100);
        server.start();

        List<EpochCorrectedPassing> received = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(3);

        AmbRc4TimingSource source = new AmbRc4TimingSource(
            "localhost", port,
            p -> { received.add(p); latch.countDown(); },
            state -> {}
        );
        source.start();

        boolean got = latch.await(10, TimeUnit.SECONDS);
        source.stop();
        server.stop();

        assertThat(got).isTrue();
        assertThat(received).hasSizeGreaterThanOrEqualTo(3);
        // Each passing should have a valid rtcTimeMicros
        received.forEach(p -> assertThat(p.rtcTimeMicros()).isPositive());
    }
}
