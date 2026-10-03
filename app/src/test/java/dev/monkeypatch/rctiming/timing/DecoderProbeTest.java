package dev.monkeypatch.rctiming.timing;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class DecoderProbeTest {

    /** STATUS per docs/AMB_DECODER_PROTOCOL.md: type, decoder_id, seq, noise, unknown, crc. */
    private static final String STATUS = "\u0001#\t20\t1\t72\t0\tx6B89\r\n";
    private static final String PASSING = "\u0001@\t20\t2\t12345\t4.000\t400\t163\t2\txDEAD\r\n";

    private final DecoderProbe probe = new DecoderProbe(Duration.ofMillis(800), Duration.ofMillis(500));

    @Test
    void validStatusRecord_isAFoundDecoder() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            Thread fake = fakeDecoder(server, STATUS);

            assertThat(probe.probe("localhost", server.getLocalPort(), "RC4").ok()).isTrue();
            fake.join();
        }
    }

    @Test
    void validPassingRecord_isAFoundDecoder() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            Thread fake = fakeDecoder(server, PASSING);

            assertThat(probe.probe("localhost", server.getLocalPort(), "RC4").ok()).isTrue();
            fake.join();
        }
    }

    @Test
    void statusWithNonNumericFields_isNotARecord() throws Exception {
        assertNoValidRecord("\u0001#\t20\t1\t0\t3.000\t0\t0\t0\r\n");
    }

    @Test
    void passingMissingFields_isNotARecord() throws Exception {
        assertNoValidRecord("\u0001@\t20\t2\tshort\r\n");
    }

    @Test
    void lineWithoutSoh_isNotARecord() throws Exception {
        assertNoValidRecord("#\t20\t1\t72\t0\tx6B89\r\n");
    }

    @Test
    void connectionOpensButNoRecords_isAFailure() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            Thread silent = new Thread(() -> {
                try (Socket ignored = server.accept()) {
                    Thread.sleep(2_000);
                } catch (IOException | InterruptedException ignored) {
                }
            });
            silent.start();

            DecoderProbe.Result result = probe.probe("localhost", server.getLocalPort(), "RC4");

            assertThat(result.ok()).isFalse();
            assertThat(result.message()).contains("no valid decoder records");
            silent.join();
        }
    }

    @Test
    void trickledBytesWithoutNewline_cannotExtendTheDeadline() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            Thread trickle = new Thread(() -> {
                try (Socket client = server.accept()) {
                    OutputStream out = client.getOutputStream();
                    for (int i = 0; i < 60; i++) {
                        out.write('x');
                        out.flush();
                        Thread.sleep(100);
                    }
                } catch (IOException | InterruptedException ignored) {
                }
            });
            trickle.start();

            long started = System.nanoTime();
            DecoderProbe.Result result = probe.probe("localhost", server.getLocalPort(), "RC4");
            long elapsedMs = (System.nanoTime() - started) / 1_000_000;

            assertThat(result.ok()).isFalse();
            assertThat(elapsedMs).isLessThan(2_000);
            trickle.join();
        }
    }

    @Test
    void nothingListening_isReportedAsConnectFailure() throws IOException {
        int port;
        try (ServerSocket free = new ServerSocket(0)) {
            port = free.getLocalPort();
        }

        DecoderProbe.Result result = probe.probe("localhost", port, "RC4");

        assertThat(result.ok()).isFalse();
        assertThat(result.message()).startsWith("Could not connect");
    }

    @Test
    void unreachableAddress_isReportedAsConnectFailure() {
        // Non-routable address: the connect either times out or fails at once. Both are connect failures.
        DecoderProbe.Result result = probe.probe("10.255.255.1", 5100, "RC4");

        assertThat(result.ok()).isFalse();
        assertThat(result.message()).startsWith("Could not connect");
    }

    @Test
    void p3Protocol_isRejectedBeforeConnecting() {
        DecoderProbe.Result result = probe.probe("localhost", 5403, "P3");

        assertThat(result.ok()).isFalse();
        assertThat(result.message()).contains("RC4");
    }

    /** Sends one line, then closes. The probe must not count it as a found decoder. */
    private void assertNoValidRecord(String line) throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            Thread fake = fakeDecoder(server, line);

            DecoderProbe.Result result = probe.probe("localhost", server.getLocalPort(), "RC4");

            assertThat(result.ok()).isFalse();
            fake.join();
        }
    }

    private static Thread fakeDecoder(ServerSocket server, String line) {
        Thread thread = new Thread(() -> {
            try (Socket client = server.accept()) {
                OutputStream out = client.getOutputStream();
                out.write(line.getBytes(StandardCharsets.US_ASCII));
                out.flush();
                Thread.sleep(300);
            } catch (IOException | InterruptedException ignored) {
            }
        });
        thread.start();
        return thread;
    }
}
