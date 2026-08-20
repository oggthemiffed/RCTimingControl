package dev.monkeypatch.rctiming.localday.config;

import dev.monkeypatch.rctiming.localday.auth.LocalSessionAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;

/**
 * Security configuration for {@code :localday} (U6). Deliberately independent of the cloud's
 * {@code app/.../security/SecurityConfig} (KTD5, KD3) — stateless, no JWT, gated by
 * {@link LocalSessionAuthenticationFilter} on plain local session tokens instead.
 *
 * <p>{@code anyRequest().authenticated()} is the default-deny stance for everything not
 * explicitly permitted. This deliberately covers future controllers this unit doesn't build yet
 * (race-control writes land in a later unit) — they fall under this rule automatically, with no
 * further security-config change required.
 *
 * <p>{@code /ws/timing} (the STOMP live-timing endpoint from {@link WebSocketConfig}, already
 * committed in a prior unit) is explicitly permitted here rather than swept into the default-deny
 * rule. R9 requires write actions to be gated but explicitly preserves an anonymous read-only
 * board/attendee view — live timing broadcast is exactly that read-only view, and
 * {@code WebSocketConfig}'s own docstring already documents this endpoint as intentionally open
 * pending a future STOMP-level auth unit. Locking it behind {@code anyRequest().authenticated()}
 * would silently break that already-shipped anonymous view.
 */
@Configuration
@EnableWebSecurity
public class LocalSecurityConfig {

    private final String cloudSyncOrigin;

    public LocalSecurityConfig(@Value("${localday.cloud-sync-origin:}") String cloudSyncOrigin) {
        this.cloudSyncOrigin = cloudSyncOrigin;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                            LocalSessionAuthenticationFilter localSessionAuthenticationFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/local-auth/**").permitAll()
                        .requestMatchers("/api/v1/boards/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/ws/timing", "/ws/timing/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(buildCspDirectives())))
                .addFilterBefore(localSessionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * A strict CSP is load-bearing here, not cosmetic: an ~18-hour full-write-privilege local
     * session token lives in {@code frontend-local}'s IndexedDB per the plan's own
     * "System-Wide Impact" security note — a loose CSP (any {@code unsafe-inline}/
     * {@code unsafe-eval}) directly weakens the blast radius of an XSS against that token.
     * {@code connect-src} gets a configurable extra origin for the future cloud-sync channel
     * (read from {@code localday.cloud-sync-origin}, empty by default so nothing is appended
     * until a later unit actually needs it).
     */
    private String buildCspDirectives() {
        String connectSrc = "connect-src 'self'";
        if (StringUtils.hasText(cloudSyncOrigin)) {
            connectSrc = connectSrc + " " + cloudSyncOrigin;
        }
        return String.join("; ",
                "default-src 'self'",
                "script-src 'self'",
                "style-src 'self'",
                connectSrc,
                "img-src 'self' data:",
                "object-src 'none'",
                "base-uri 'self'"
        );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
