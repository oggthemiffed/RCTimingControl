package dev.monkeypatch.rctiming.simulator;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** How the simulator's modes send RC-4 text records and wait between them. */
final class Rc4Lines {

    /** Start-of-header byte the decoder puts before every record. */
    private static final int SOH = 0x01;

    private Rc4Lines() {
    }

    /** Sends one record as the decoder does: SOH, the tab-separated line, CRLF. */
    static void write(OutputStream out, String line) throws IOException {
        out.write(SOH);
        out.write(line.getBytes(StandardCharsets.US_ASCII));
        out.write('\r');
        out.write('\n');
        out.flush();
    }

    /** Waits, keeping the thread's interrupt so the mode's loop stops. */
    static void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
