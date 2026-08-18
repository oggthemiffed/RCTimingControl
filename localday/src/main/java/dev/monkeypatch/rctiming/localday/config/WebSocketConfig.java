package dev.monkeypatch.rctiming.localday.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP WebSocket configuration. Endpoint: {@code /ws/timing} — no SockJS (root CLAUDE.md
 * forbids SockJS; venue LAN does not need a fallback transport).
 *
 * <p>Ported from the cloud's {@code app/.../config/websocket/WebSocketConfig.java} (KD3). The
 * cloud injects a {@code JwtChannelInterceptor} here — {@code :localday} has no local auth yet
 * (a later unit, U6, adds a day-scoped local credential), so no interceptor is added here,
 * matching this unit's actual scope: the STOMP endpoint is left open for now.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/timing");
        // No .withSockJS() — CLAUDE.md forbids SockJS
    }
}
