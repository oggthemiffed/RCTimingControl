package dev.monkeypatch.rctiming.forwarder.grpc;

import dev.monkeypatch.rctiming.forwarder.config.ForwarderConfig;
import dev.monkeypatch.rctiming.forwarder.proto.ForwarderCommand;
import dev.monkeypatch.rctiming.forwarder.proto.LapPassing;
import dev.monkeypatch.rctiming.forwarder.proto.TimingServiceGrpc;
import dev.monkeypatch.rctiming.forwarder.timing.EpochCorrectedPassing;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Phase 5 / FORWARDER-03: gRPC client for streaming LapPassing events to the cloud service.
 *
 * <p>FORWARDER-03 RESEND clause: The RC-4 text protocol has no RESEND mechanism.
 * ForwarderCommand proto is defined for future P3 RESEND use. In Phase 5,
 * the cloud sends no RESEND commands; the response observer simply logs
 * any received ForwarderCommand for diagnostics.
 *
 * <p>T-05-20: Token transmitted without TLS on venue LAN (accepted risk for v1).
 * For production: set grpcPlaintext=false and provision TLS certificates.
 *
 * <p>Thread safety: {@link #sendPassing} is synchronized — safe to call from any thread.
 * The forwarder uses a single parsing thread, so no contention in practice.
 */
public class ForwarderGrpcClient {

    private static final Logger log = LoggerFactory.getLogger(ForwarderGrpcClient.class);

    static final Metadata.Key<String> TOKEN_KEY =
            Metadata.Key.of("x-forwarder-token", Metadata.ASCII_STRING_MARSHALLER);

    private final String host;
    private final int port;
    private final boolean plaintext;
    private final String apiToken;

    private ManagedChannel channel;
    private StreamObserver<LapPassing> requestObserver;
    private volatile boolean closed = false;

    public ForwarderGrpcClient(String host, int port, boolean plaintext, String apiToken) {
        this.host = host;
        this.port = port;
        this.plaintext = plaintext;
        this.apiToken = apiToken;
    }

    /** Factory method: construct from ForwarderConfig. */
    public static ForwarderGrpcClient fromConfig(ForwarderConfig cfg) {
        return new ForwarderGrpcClient(
                cfg.grpcHost(), cfg.grpcPort(), cfg.grpcPlaintext(), cfg.apiToken());
    }

    /**
     * Opens the gRPC channel and starts the StreamPassings RPC.
     * Attaches the API token as x-forwarder-token metadata on every call.
     */
    public synchronized void connect() {
        if (closed) return;

        ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forAddress(host, port);
        if (plaintext) {
            builder.usePlaintext();
        }
        channel = builder.build();

        // Attach token as metadata via ClientInterceptor (T-05-20)
        TimingServiceGrpc.TimingServiceStub stub = TimingServiceGrpc.newStub(channel)
                .withInterceptors(new ClientInterceptor() {
                    @Override
                    public <Q, R> ClientCall<Q, R> interceptCall(
                            MethodDescriptor<Q, R> method, CallOptions opts, Channel next) {
                        return new ForwardingClientCall.SimpleForwardingClientCall<>(
                                next.newCall(method, opts)) {
                            @Override
                            public void start(Listener<R> responseListener, Metadata headers) {
                                headers.put(TOKEN_KEY, apiToken);
                                super.start(responseListener, headers);
                            }
                        };
                    }
                });

        requestObserver = stub.streamPassings(new StreamObserver<>() {
            @Override
            public void onNext(ForwarderCommand cmd) {
                // Phase 5: RESEND not implemented for RC-4; log for diagnostics only
                log.debug("Received ForwarderCommand from cloud: {}", cmd);
            }

            @Override
            public void onError(Throwable t) {
                log.warn("gRPC stream error — scheduling reconnect: {}", t.getMessage());
                if (!closed) {
                    scheduleReconnect();
                }
            }

            @Override
            public void onCompleted() {
                log.info("gRPC stream completed by cloud");
                if (!closed) {
                    scheduleReconnect();
                }
            }
        });

        log.info("gRPC stream connected to {}:{}", host, port);
    }

    private void scheduleReconnect() {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            if (!closed) {
                log.info("gRPC reconnecting to {}:{}...", host, port);
                connect();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /**
     * Converts an EpochCorrectedPassing to a LapPassing proto message and streams it to the cloud.
     * Thread-safe; drops the passing silently if the stream is not ready.
     */
    public synchronized void sendPassing(EpochCorrectedPassing p) {
        if (requestObserver == null || closed) {
            log.debug("gRPC stream not ready — dropping passing for {}", p.transponderNumber());
            return;
        }
        try {
            LapPassing proto = LapPassing.newBuilder()
                    .setTransponderNumber(p.transponderNumber())
                    .setRtcTimeMicros(p.rtcTimeMicros())
                    .setDecoderId(p.decoderId())
                    .setSeqNum(p.seqNum())
                    .setSignalStrength(p.signalStrength())
                    .setHitCount(p.hitCount())
                    .build();
            requestObserver.onNext(proto);
        } catch (Exception e) {
            log.warn("Failed to send passing for {}: {}", p.transponderNumber(), e.getMessage());
        }
    }

    /** Gracefully closes the stream and shuts down the channel. */
    public synchronized void close() {
        closed = true;
        if (requestObserver != null) {
            try {
                requestObserver.onCompleted();
            } catch (Exception ignored) {}
        }
        if (channel != null) {
            channel.shutdown();
        }
    }
}
