package dev.monkeypatch.rctiming.forwarder.simulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Entry point for the standalone AMB RC-4 TCP decoder simulator.
 *
 * <h3>Usage</h3>
 * <pre>
 * ./gradlew :forwarder:runSimulator --args="--mode=playback --port=5100 --speed=1.0"
 * ./gradlew :forwarder:runSimulator --args="--mode=generative --port=5100 --transponders=11111,22222,33333 --interval-ms=12500 --jitter-ms=2500"
 * </pre>
 *
 * <h3>Flags</h3>
 * <ul>
 *   <li>{@code --mode} — {@code playback} or {@code generative} (required)</li>
 *   <li>{@code --port} — TCP port to listen on (default: 5100)</li>
 *   <li>{@code --speed} — playback speed factor (default: 1.0; playback mode only)</li>
 *   <li>{@code --transponders} — comma-separated transponder IDs (default: 11111,22222; generative mode only)</li>
 *   <li>{@code --interval-ms} — base lap time in milliseconds (default: 12500; generative mode only)</li>
 *   <li>{@code --jitter-ms} — maximum random lap-time deviation; each lap is intervalMs ± rand(0, jitterMs) (default: 2500; generative only)</li>
 * </ul>
 *
 * <p>Invalid or missing required arguments cause usage to be printed to stderr and exit code 2.
 */
public class SimulatorMain {

    private static final Logger log = LoggerFactory.getLogger(SimulatorMain.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        Map<String, String> flags = parseFlags(args);

        String mode = flags.get("--mode");
        if (mode == null) {
            printUsageAndExit("--mode is required");
        }

        int    port       = parseInt(flags, "--port",         5100);
        double speed      = parseDouble(flags, "--speed",     1.0);
        long   intervalMs = parseLong(flags, "--interval-ms", 12_500L);
        long   jitterMs   = parseLong(flags, "--jitter-ms",   2_500L);

        List<String> transponders = new ArrayList<>();
        String txpRaw = flags.getOrDefault("--transponders", "11111,22222");
        for (String t : txpRaw.split(",")) {
            String trimmed = t.trim();
            if (!trimmed.isEmpty()) transponders.add(trimmed);
        }

        FakeDecoderServer server;
        switch (mode) {
            case "playback"   -> server = FakeDecoderServer.playback(port, flags.get("--file"), speed);
            case "generative" -> server = FakeDecoderServer.generative(port, transponders, intervalMs, jitterMs);
            default -> { printUsageAndExit("Unknown --mode: " + mode); return; }
        }

        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        log.info("[SIMULATOR] Running — Ctrl-C to stop");
        Thread.currentThread().join(); // block forever
    }

    // -------------------------------------------------------------------------
    // CLI parsing helpers
    // -------------------------------------------------------------------------

    private static Map<String, String> parseFlags(String[] args) {
        java.util.HashMap<String, String> map = new java.util.HashMap<>();
        for (String arg : args) {
            if (!arg.startsWith("--")) {
                System.err.println("Unknown argument: " + arg);
                printUsageAndExit(null);
            }
            int eq = arg.indexOf('=');
            if (eq < 0) {
                map.put(arg, "true");
            } else {
                map.put(arg.substring(0, eq), arg.substring(eq + 1));
            }
        }
        return map;
    }

    private static int parseInt(Map<String, String> flags, String key, int def) {
        try { return flags.containsKey(key) ? Integer.parseInt(flags.get(key)) : def; }
        catch (NumberFormatException e) { printUsageAndExit(key + " must be an integer"); return def; }
    }

    private static long parseLong(Map<String, String> flags, String key, long def) {
        try { return flags.containsKey(key) ? Long.parseLong(flags.get(key)) : def; }
        catch (NumberFormatException e) { printUsageAndExit(key + " must be a long integer"); return def; }
    }

    private static double parseDouble(Map<String, String> flags, String key, double def) {
        try { return flags.containsKey(key) ? Double.parseDouble(flags.get(key)) : def; }
        catch (NumberFormatException e) { printUsageAndExit(key + " must be a number"); return def; }
    }

    private static void printUsageAndExit(String error) {
        if (error != null) System.err.println("Error: " + error);
        System.err.println("""
            Usage:
              --mode=playback|generative  (required)
              --port=5100                 TCP port (default: 5100)
              --speed=1.0                 Playback speed factor (playback only)
              --file=/path/to/dump        Dump file path (playback only; default: classpath sample)
              --transponders=11111,22222  Transponder IDs (generative only)
              --interval-ms=12500         Base lap time in ms (default: 12500)
              --jitter-ms=2500            Max random lap-time deviation ms (default: 2500)
            """);
        System.exit(2);
    }
}
