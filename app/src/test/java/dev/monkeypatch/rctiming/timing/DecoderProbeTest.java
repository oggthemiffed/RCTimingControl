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

    private static final String STATUS = "\u0001#\t20\t1\t0\t3.000\t0\t0\t0\r\n";
    private static final String PASSING = "\u0001@\t20\t2\t12345\t4.000\t400\t163\t2\txDEAD\r\n";

    private final DecoderProbe probe = new DecoderProbe(Duration.ofMillis(800));

    @Test
    void rc4StatusRecord_isAFoundDecoder() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            Thread fake = fakeDecoder(server, STATUS);

            DecoderProbe.Result result = probe.probe("localhost", server.getLocalPort(), "RC4");

            assertThat(result.ok()).isTrue();
            fake.join();
        }
    }

    @Test
    void passingRecord_alsoCountsAsFound() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            Thread fake = fakeDecoder(server, PASSING);

            assertThat(probe.probe("localhost", server.getLocalPort(), "RC4").ok()).isTrue();
            fake.join();
        }
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
            assertThat(result.message()).contains("no decoder records");
            silent.join();
        }
    }

    @Test
    void nothingListening_isAFailure() throws IOException {
        int port;
        try (ServerSocket free = new ServerSocket(0)) {
            port = free.getLocalPort();
        }

        DecoderProbe.Result result = probe.probe("localhost", port, "RC4");

        assertThat(result.ok()).isFalse();
        assertThat(result.message()).contains("Could not connect");
    }

    @Test
    void p3Protocol_isRejectedBeforeConnecting() {
        DecoderProbe.Result result = probe.probe("localhost", 5403, "P3");

        assertThat(result.ok()).isFalse();
        assertThat(result.message()).contains("RC4");
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
