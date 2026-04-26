package dev.monkeypatch.rctiming.forwarder;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Phase 5 / FORWARDER-03: SmartLifecycle gRPC server that starts/stops with the Spring context.
 * Binds on app.grpc.port (default 9090) with ForwarderTokenAuthInterceptor validating every
 * incoming stream before ForwarderGrpcService processes any messages.
 */
@Component
public class ForwarderGrpcServer implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(ForwarderGrpcServer.class);

    private final ForwarderGrpcService grpcService;
    private final ForwarderTokenAuthInterceptor authInterceptor;

    @Value("${app.grpc.port:9090}")
    private int grpcPort;

    private Server server;

    public ForwarderGrpcServer(ForwarderGrpcService grpcService,
                               ForwarderTokenAuthInterceptor authInterceptor) {
        this.grpcService = grpcService;
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void start() {
        try {
            server = ServerBuilder.forPort(grpcPort)
                    .addService(grpcService)
                    .intercept(authInterceptor)
                    .build()
                    .start();
            log.info("gRPC forwarder server started on port {}", grpcPort);
        } catch (IOException e) {
            throw new RuntimeException("Failed to start gRPC server on port " + grpcPort, e);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            server.shutdown();
            log.info("gRPC forwarder server stopped");
        }
    }

    @Override
    public boolean isRunning() {
        return server != null && !server.isShutdown();
    }
}
