package dev.monkeypatch.rctiming.forwarder.simulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simple TCP server that emits RC-4 text frames to connected clients.
 *
 * <p><strong>[SIMULATOR] DEV-ONLY — do not run on production hosts</strong>
 *
 * <p>Supports two operating modes:
 * <ul>
 *   <li>{@link #playback(int, String, double)} — replays a {@code .dump} file at configurable speed</li>
 *   <li>{@link #generative(int, List, long)} — emits synthetic PASSING records for a list of transponders</li>
 * </ul>
 *
 * <p>Each accepted TCP connection is handled on a separate daemon thread.
 * Start via {@link #start()} and stop via {@link #stop()}.
 */
public class FakeDecoderServer {

    private static final Logger log = LoggerFactory.getLogger(FakeDecoderServer.class);

    public enum Mode { PLAYBACK, GENERATIVE }

    private final int    port;
    private final Mode   mode;

    // Playback mode config
    private final String  playbackFile;
    private final double  speed;

    // Generative mode config
    private final List<String> transponders;
    private final long         intervalMs;

    private ServerSocket     serverSocket;
    private ExecutorService  acceptorThread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    // Track active client sockets so we can close them on stop()
    private final CopyOnWriteArrayList<Socket> activeClients = new CopyOnWriteArrayList<>();

    // Private constructor — use factory methods
    private FakeDecoderServer(int port, Mode mode,
                              String playbackFile, double speed,
                              List<String> transponders, long intervalMs) {
        this.port         = port;
        this.mode         = mode;
        this.playbackFile = playbackFile;
        this.speed        = speed;
        this.transponders = transponders;
        this.intervalMs   = intervalMs;
    }

    /** Create a playback-mode server that replays a {@code .dump} file. */
    public static FakeDecoderServer playback(int port, String dumpFilePath, double speed) {
        return new FakeDecoderServer(port, Mode.PLAYBACK, dumpFilePath, speed, List.of(), 0);
    }

    /** Create a generative-mode server emitting synthetic PASSING records. */
    public static FakeDecoderServer generative(int port, List<String> transponders, long intervalMs) {
        return new FakeDecoderServer(port, Mode.GENERATIVE, null, 1.0, transponders, intervalMs);
    }

    /**
     * Start listening on the configured port.
     *
     * @throws IOException if the port cannot be bound
     */
    public void start() throws IOException {
        log.info("[SIMULATOR] DEV-ONLY — do not run on production hosts");
        log.info("[SIMULATOR] Starting FakeDecoderServer on port {} ({})", port, mode);
        serverSocket   = new ServerSocket(port);
        running.set(true);
        acceptorThread = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "fake-decoder-acceptor-" + port);
            t.setDaemon(true);
            return t;
        });
        acceptorThread.submit(this::acceptLoop);
    }

    /** Stop the server and close all resources. Idempotent. */
    public void stop() {
        running.set(false);
        // Close all active client sockets so their serving threads exit promptly
        for (Socket client : activeClients) {
            try { client.close(); } catch (IOException ignored) {}
        }
        activeClients.clear();
        if (serverSocket != null && !serverSocket.isClosed()) {
            try { serverSocket.close(); } catch (IOException ignored) {}
        }
        if (acceptorThread != null) {
            acceptorThread.shutdownNow();
        }
        log.info("[SIMULATOR] FakeDecoderServer stopped (port {})", port);
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                Socket client = serverSocket.accept();
                log.info("[SIMULATOR] Client connected: {}", client.getRemoteSocketAddress());
                activeClients.add(client);
                Thread clientThread = new Thread(() -> serveClient(client),
                                                 "fake-decoder-client-" + port);
                clientThread.setDaemon(true);
                clientThread.start();
            } catch (IOException e) {
                if (running.get()) {
                    log.warn("[SIMULATOR] Accept error: {}", e.getMessage());
                }
            }
        }
    }

    private void serveClient(Socket client) {
        try (OutputStream out = client.getOutputStream()) {
            switch (mode) {
                case PLAYBACK   -> PlaybackMode.replay(out, playbackFile, speed);
                case GENERATIVE -> GenerativeMode.run(out, transponders, intervalMs);
            }
        } catch (IOException e) {
            if (running.get()) {
                log.warn("[SIMULATOR] Client disconnected: {}", e.getMessage());
            }
        } finally {
            activeClients.remove(client);
            try { client.close(); } catch (IOException ignored) {}
        }
    }
}
