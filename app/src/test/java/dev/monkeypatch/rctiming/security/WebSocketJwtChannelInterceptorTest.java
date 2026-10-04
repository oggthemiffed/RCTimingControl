package dev.monkeypatch.rctiming.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.security.Principal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WebSocketJwtChannelInterceptorTest {

    private JwtTokenService jwtTokenService;
    private WebSocketJwtChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        jwtTokenService = mock(JwtTokenService.class);
        interceptor = new WebSocketJwtChannelInterceptor(jwtTokenService);
    }

    @Test
    void connectWithAValidToken_setsTheUser() {
        when(jwtTokenService.parseToken("good"))
                .thenReturn(Jwts.claims().subject("42").add("roles", List.of("RACE_DIRECTOR")).build());

        Message<?> result = interceptor.preSend(connect("Bearer good"), null);

        assertThat(result).isNotNull();
        Principal user = accessor(result).getUser();
        assertThat(user).isNotNull();
        assertThat(user.getName()).isEqualTo("42");
    }

    @Test
    void connectWithNoToken_isAnAnonymousSpectator() {
        Message<?> result = interceptor.preSend(connect(null), null);

        assertThat(result).isNotNull();
        assertThat(accessor(result).getUser()).isNull();
        verifyNoInteractions(jwtTokenService);
    }

    @Test
    void connectWithAnEmptyBearer_isAnAnonymousSpectator() {
        assertThat(interceptor.preSend(connect("Bearer "), null)).isNotNull();
        assertThat(interceptor.preSend(connect("Bearer"), null)).isNotNull();
        verifyNoInteractions(jwtTokenService);
    }

    @Test
    void connectWithAnInvalidToken_isRejected() {
        when(jwtTokenService.parseToken(anyString())).thenThrow(new MalformedJwtException("bad"));

        assertThat(interceptor.preSend(connect("Bearer garbage"), null)).isNull();
    }

    @Test
    void connectWithAMalformedHeader_isRejected() {
        assertThat(interceptor.preSend(connect("Basic dXNlcjpwYXNz"), null)).isNull();
    }

    @Test
    void anonymousSubscribe_isLimitedToRaceTimingAndState() {
        assertThat(interceptor.preSend(subscribe("/topic/race/7/timing", null), null)).isNotNull();
        assertThat(interceptor.preSend(subscribe("/topic/race/7/state", null), null)).isNotNull();

        assertThat(interceptor.preSend(subscribe("/topic/race/7/marshal", null), null)).isNull();
        assertThat(interceptor.preSend(subscribe("/topic/race/7/unknown-transponder", null), null)).isNull();
        assertThat(interceptor.preSend(subscribe("/topic/race/7/audio", null), null)).isNull();
        assertThat(interceptor.preSend(subscribe("/topic/system/decoder-status", null), null)).isNull();
        assertThat(interceptor.preSend(subscribe("/topic/practice/3/timing", null), null)).isNull();
        assertThat(interceptor.preSend(subscribe("/topic/race/7/timing/extra", null), null)).isNull();
        assertThat(interceptor.preSend(subscribe("/topic/race/*/timing", null), null)).isNull();
        assertThat(interceptor.preSend(subscribe(null, null), null)).isNull();
    }

    @Test
    void authenticatedSubscribe_isUnrestricted() {
        assertThat(interceptor.preSend(subscribe("/topic/race/7/marshal", official()), null)).isNotNull();
        assertThat(interceptor.preSend(subscribe("/topic/system/decoder-status", official()), null)).isNotNull();
    }

    @Test
    void anonymousSend_isRejected() {
        assertThat(interceptor.preSend(send("/topic/race/7/timing", null), null)).isNull();
        assertThat(interceptor.preSend(send("/app/anything", null), null)).isNull();
    }

    @Test
    void authenticatedSend_isPassedThrough() {
        assertThat(interceptor.preSend(send("/app/anything", official()), null)).isNotNull();
    }

    private static UsernamePasswordAuthenticationToken official() {
        return new UsernamePasswordAuthenticationToken("1", null, List.of());
    }

    private static Message<byte[]> connect(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        return build(accessor);
    }

    private static Message<byte[]> subscribe(String destination, Principal user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setUser(user);
        return build(accessor);
    }

    private static Message<byte[]> send(String destination, Principal user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination(destination);
        accessor.setUser(user);
        return build(accessor);
    }

    private static Message<byte[]> build(StompHeaderAccessor accessor) {
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private static StompHeaderAccessor accessor(Message<?> message) {
        return MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    }
}
