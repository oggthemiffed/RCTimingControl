package dev.monkeypatch.rctiming.timing;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Tests a decoder address the operator has typed in, without touching the live listener.
 *
 * <p>Opens a TCP connection and waits for one RC-4 record. The decoder sends a STATUS record
 * every few seconds, so a record within the timeout means the address is a working decoder on
 * the right protocol. A connection that opens but sends nothing is reported as a failure.
 */
@Component
public class DecoderProbe {

    /** Long enough to catch the decoder's 5-second STATUS heartbeat. */
    static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(8);

    private static final int CONNECT_TIMEOUT_MS = 5_000;

    private final Duration timeout;

    public DecoderProbe() {
        this(DEFAULT_TIMEOUT);
    }

    DecoderProbe(Duration timeout) {
        this.timeout = timeout;
    }

    public Result probe(String host, int port, String protocol) {
        if (!"RC4".equals(protocol)) {
            return new Result(false, "Only the RC4 protocol is supported at the moment.");
        }
        InetSocketAddress address = new InetSocketAddress(host, port);
        if (address.isUnresolved()) {
            return new Result(false, "Could not find a host called " + host + ".");
        }

        try (Socket socket = new Socket()) {
            socket.connect(address, CONNECT_TIMEOUT_MS);
            socket.setSoTimeout((int) timeout.toMillis());
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));

            long deadline = System.nanoTime() + timeout.toNanos();
            while (System.nanoTime() < deadline) {
                String line = reader.readLine();
                if (line == null) {
                    return new Result(false, "The decoder closed the connection without sending data.");
                }
                if (isRc4Record(line)) {
                    return new Result(true, "Decoder found at " + host + ":" + port + ".");
                }
            }
            return new Result(false, "Connected, but no decoder records arrived. Check the protocol and port.");
        } catch (SocketTimeoutException e) {
            return new Result(false, "Connected, but no decoder records arrived. Check the protocol and port.");
        } catch (IOException e) {
            return new Result(false, "Could not connect to " + host + ":" + port + ". Check the address and that the decoder is on.");
        }
    }

    /** RC-4 records start with SOH (0x01) then '#' (STATUS) or '@' (PASSING). */
    private static boolean isRc4Record(String line) {
        if (line.isEmpty() || line.charAt(0) != 0x01) {
            return false;
        }
        return line.length() > 1 && (line.charAt(1) == '#' || line.charAt(1) == '@');
    }

    /** Outcome of a probe. {@code message} is shown to the operator. */
    public record Result(boolean ok, String message) {}
}
