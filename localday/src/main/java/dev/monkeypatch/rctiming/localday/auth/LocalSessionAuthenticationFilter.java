package dev.monkeypatch.rctiming.localday.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Resolves {@code Authorization: Bearer <sessionToken>} into a Spring Security authentication
 * for the request. Structurally mirrors app's {@code JwtAuthenticationFilter}, adapted to this
 * module's plain session-token lookup instead of JWT parsing (KTD5 — no shared code, no shared
 * session model between {@code :app} and {@code :localday}).
 *
 * <p>This module has no stacked-role model yet — every valid session gets a single
 * {@code ROLE_OFFICIAL} authority; R9 only distinguishes "authenticated official" from
 * "anonymous," not fine-grained roles like the cloud's ADMIN/RACE_DIRECTOR/REFEREE.
 */
@Component
public class LocalSessionAuthenticationFilter extends OncePerRequestFilter {

    private final LocalSessionService localSessionService;

    public LocalSessionAuthenticationFilter(LocalSessionService localSessionService) {
        this.localSessionService = localSessionService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getServletPath().startsWith("/api/v1/local-auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            Optional<SessionPrincipal> principal = localSessionService.validateSession(token);
            if (principal.isPresent()) {
                List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_OFFICIAL"));
                var auth = new UsernamePasswordAuthenticationToken(
                        principal.get().officialName(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
            // Invalid/expired/unknown token: do not set the context; Spring Security's own
            // entry point rejects the request if the target endpoint requires authentication.
        }
        chain.doFilter(request, response);
    }
}
