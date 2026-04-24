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

/**
 * Validates JWT on STOMP CONNECT frames (Pattern 3 from RESEARCH.md).
 * HTTP upgrade itself is permitAll (Pitfall 1 — JWT at CONNECT, not at HTTP level).
 * Unauthenticated CONNECTs return null to reject the frame.
 */
@Component
public class WebSocketJwtChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketJwtChannelInterceptor.class);

    private final JwtTokenService jwtTokenService;

    public WebSocketJwtChannelInterceptor(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("STOMP CONNECT rejected: missing or malformed Authorization header");
                return null;
            }
            String token = authHeader.substring(7);
            try {
                Claims claims = jwtTokenService.parseToken(token);
                List<String> roles = claims.get("roles", List.class);
                List<GrantedAuthority> authorities = (roles != null ? roles : List.<String>of()).stream()
                        .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r))
                        .toList();
                var auth = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(), null, authorities);
                accessor.setUser(auth);
            } catch (JwtException e) {
                log.warn("STOMP CONNECT rejected: invalid JWT — {}", e.getMessage());
                return null;
            }
        }
        return message;
    }
}
