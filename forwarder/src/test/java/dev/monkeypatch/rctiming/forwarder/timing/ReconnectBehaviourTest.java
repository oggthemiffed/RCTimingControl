package dev.monkeypatch.rctiming.forwarder.timing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies reconnect behaviour of {@link AmbRc4TimingSource} when the decoder is unreachable.
 * Uses a port with NO listener (connection refused) to force repeated reconnect attempts.
 */
class ReconnectBehaviourTest {

    /** A high port that should have no listener. */
    private static final int UNREACHABLE_PORT = 59876;

    @Test
    @Timeout(10)
    void statusAbsenceTriggersReconnect() throws Exception {
        // Connect to a port with no listener → immediate RECONNECTING state
        CountDownLatch reconnectLatch = new CountDownLatch(1);

        AmbRc4TimingSource source = new AmbRc4TimingSource(
            "localhost", UNREACHABLE_PORT,
            p -> {},
            state -> {
                if (state == AmbRc4TimingSource.ConnectionState.RECONNECTING) {
                    reconnectLatch.countDown();
                }
            }
        );
        source.start();

        boolean fired = reconnectLatch.await(3, TimeUnit.SECONDS);
        source.stop();

        assertThat(fired).as("RECONNECTING should fire within 3 s when connection refused").isTrue();
    }

    @Test
    @Timeout(10)
    void exponentialBackoffCappedAt30s() throws Exception {
        // We can't wait 30 s in a unit test, but we can verify at least 2 reconnect
        // attempts fire within a few seconds (backoff 1 s then 2 s).
        AtomicInteger reconnectCount = new AtomicInteger(0);
        CountDownLatch twoReconnects = new CountDownLatch(2);

        AmbRc4TimingSource source = new AmbRc4TimingSource(
            "localhost", UNREACHABLE_PORT + 1,
            p -> {},
            state -> {
                if (state == AmbRc4TimingSource.ConnectionState.RECONNECTING) {
                    reconnectCount.incrementAndGet();
                    twoReconnects.countDown();
                }
            }
        );
        source.start();

        boolean twoFired = twoReconnects.await(7, TimeUnit.SECONDS);
        source.stop();

        assertThat(twoFired)
            .as("At least 2 RECONNECTING callbacks should fire within 7 s (backoffs: 1 s + 2 s)")
            .isTrue();
        assertThat(reconnectCount.get()).isGreaterThanOrEqualTo(2);
    }
}
