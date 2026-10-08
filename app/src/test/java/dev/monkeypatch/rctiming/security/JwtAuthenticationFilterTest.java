package dev.monkeypatch.rctiming.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private final JwtTokenService jwtTokenService = mock(JwtTokenService.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenService);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void aTokenWithRoles_setsTheirAuthorities() throws Exception {
        when(jwtTokenService.parseToken("token"))
                .thenReturn(Jwts.claims().subject("42").add("roles", List.of("ADMIN", "REFEREE")).build());

        Authentication auth = filterWithBearer("token");

        assertThat(auth.getName()).isEqualTo("42");
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN", "ROLE_REFEREE");
    }

    @Test
    void aTokenWithNoRolesClaim_isSignedInWithNoAuthorities() throws Exception {
        when(jwtTokenService.parseToken("token")).thenReturn(Jwts.claims().subject("42").build());

        Authentication auth = filterWithBearer("token");

        assertThat(auth.getName()).isEqualTo("42");
        assertThat(auth.getAuthorities()).isEmpty();
    }

    @Test
    void anInvalidToken_leavesTheRequestAnonymous() throws Exception {
        when(jwtTokenService.parseToken("token")).thenThrow(new JwtException("bad signature"));

        assertThat(filterWithBearer("token")).isNull();
    }

    private Authentication filterWithBearer(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/audio/voices");
        request.addHeader("Authorization", "Bearer " + token);
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        assertThat(chain.getRequest()).isSameAs(request);
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
