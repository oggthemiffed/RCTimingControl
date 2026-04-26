package dev.monkeypatch.rctiming.forwarder.timing;

import java.util.Optional;

/**
 * Pure parser for the AMB RC-4 text protocol (FORWARDER-02, D-03).
 *
 * <p>This class has no static state and no Spring dependencies. It converts a single
 * RC-4 text line into a {@link ParsedPassing}. The SOH (0x01) prefix byte MUST be
 * stripped by the caller (Rc4InboundHandler) before invoking {@link #parse(String)}.
 *
 * <p>Handles two record types:
 * <ul>
 *   <li>{@code @} — PASSING record; returns a populated {@link ParsedPassing}</li>
 *   <li>{@code #} — STATUS/heartbeat; returns empty</li>
 *   <li>Any other input — returns empty (T-05-03 malformed-frame mitigation)</li>
 * </ul>
 */
public class Rc4TextParser {

    /**
     * Parse a single RC-4 text line (SOH already stripped).
     *
     * @param line the tab-separated line (no leading SOH, no trailing CRLF)
     * @return populated {@link ParsedPassing} for PASSING records; empty for STATUS or malformed
     */
    public Optional<ParsedPassing> parse(String line) {
        if (line == null || line.isEmpty()) return Optional.empty();
        String[] f = line.split("\t");
        if (f.length < 1) return Optional.empty();
        return switch (f[0]) {
            case "@" -> parsePassing(f);
            case "#" -> Optional.empty();
            default  -> Optional.empty();
        };
    }

    private Optional<ParsedPassing> parsePassing(String[] f) {
        // Fields: @, decoderId, seqNum, transponderId, timeSinceStart, hits, strength, status, crc
        if (f.length < 9) return Optional.empty();
        try {
            int decoderId   = Integer.parseInt(f[1]);
            int seqNum      = Integer.parseInt(f[2]);
            String transponder = f[3];           // kept as String per LapPassingEvent contract
            double timeSinceStart = Double.parseDouble(f[4]);
            int hitCount    = Integer.parseInt(f[5]);
            int strength    = Integer.parseInt(f[6]);
            // f[7] = passingStatus, f[8] = crc — not stored in ParsedPassing
            return Optional.of(new ParsedPassing(transponder, timeSinceStart, seqNum, decoderId, strength, hitCount));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
