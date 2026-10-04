package dev.monkeypatch.rctiming.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates JWT on STOMP CONNECT frames (Pattern 3 from RESEARCH.md).
 * HTTP upgrade itself is permitAll (Pitfall 1 — JWT at CONNECT, not at HTTP level).
 *
 * <p>A CONNECT with no token (or an empty bearer) is accepted as an anonymous
 * spectator session (L12 spectator boards). Anonymous sessions may only
 * SUBSCRIBE to a race's live timing and state topics and may never SEND. A CONNECT
 * carrying a non-empty but invalid token is still rejected (returns null).
 */
@Component
public class WebSocketJwtChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketJwtChannelInterceptor.class);

    /** Topics an anonymous spectator may subscribe to. */
    static final Pattern ANONYMOUS_TOPICS = Pattern.compile("^/topic/race/\\d+/(timing|state)$");

    private final JwtTokenService jwtTokenService;

    public WebSocketJwtChannelInterceptor(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        return switch (accessor.getCommand()) {
            case CONNECT, STOMP -> handleConnect(message, accessor);
            case SUBSCRIBE -> handleSubscribe(message, accessor);
            case SEND -> handleSend(message, accessor);
            default -> message;
        };
    }

    private Message<?> handleConnect(Message<?> message, StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || authHeader.isBlank() || authHeader.strip().equals("Bearer")) {
            // Anonymous spectator: no user is set; SUBSCRIBE/SEND checks restrict it.
            return message;
        }
        if (!authHeader.startsWith("Bearer ")) {
            log.warn("STOMP CONNECT rejected: malformed Authorization header");
            return null;
        }
        String token = authHeader.substring(7).strip();
        if (token.isEmpty()) {
            return message;
        }
        try {
            Claims claims = jwtTokenService.parseToken(token);
            List<String> roles = claims.get("roles", List.class);
            List<GrantedAuthority> authorities = (roles != null ? roles : List.<String>of()).stream()
                    .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r))
                    .toList();
            var auth = new UsernamePasswordAuthenticationToken(
                    claims.getSubject(), null, authorities);
            accessor.setUser(auth);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("STOMP CONNECT rejected: invalid JWT — {}", e.getMessage());
            return null;
        }
        return message;
    }

    private Message<?> handleSubscribe(Message<?> message, StompHeaderAccessor accessor) {
        if (accessor.getUser() != null) {
            return message;
        }
        String destination = accessor.getDestination();
        if (destination != null && ANONYMOUS_TOPICS.matcher(destination).matches()) {
            return message;
        }
        log.debug("STOMP SUBSCRIBE rejected for anonymous session: {}", destination);
        return null;
    }

    private Message<?> handleSend(Message<?> message, StompHeaderAccessor accessor) {
        if (accessor.getUser() != null) {
            return message;
        }
        log.debug("STOMP SEND rejected for anonymous session: {}", accessor.getDestination());
        return null;
    }
}
