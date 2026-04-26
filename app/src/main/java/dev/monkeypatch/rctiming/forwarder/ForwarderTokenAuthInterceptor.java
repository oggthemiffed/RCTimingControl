package dev.monkeypatch.rctiming.forwarder;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.springframework.stereotype.Component;

/**
 * Phase 5 / T-05-14: gRPC server interceptor that validates the x-forwarder-token metadata
 * against ForwarderTokenService.validate(). Rejects unauthenticated streams with UNAUTHENTICATED
 * status before any business logic runs. Handles T-05-15 (replay of revoked token) because
 * validate() only matches ACTIVE tokens — revoked tokens fail on every reconnect attempt.
 *
 * <p>T-05-20: Token is transmitted without TLS on venue LAN. This is accepted for v1;
 * see TLS upgrade path note in production deployment guide.
 */
@Component
public class ForwarderTokenAuthInterceptor implements ServerInterceptor {

    static final Metadata.Key<String> TOKEN_KEY =
            Metadata.Key.of("x-forwarder-token", Metadata.ASCII_STRING_MARSHALLER);

    private final ForwarderTokenService tokenService;

    public ForwarderTokenAuthInterceptor(ForwarderTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public <Q, R> ServerCall.Listener<Q> interceptCall(
            ServerCall<Q, R> call, Metadata headers, ServerCallHandler<Q, R> next) {
        String token = headers.get(TOKEN_KEY);
        if (tokenService.validate(token).isEmpty()) {
            call.close(
                    Status.UNAUTHENTICATED.withDescription("Invalid or missing forwarder token"),
                    new Metadata());
            return new ServerCall.Listener<>() {};
        }
        return next.startCall(call, headers);
    }
}
