package dev.monkeypatch.rctiming.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/setup/status").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/setup/bootstrap").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/events", "/api/v1/events/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/results/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/championships/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/about").permitAll()
                        // Club logos and TTS clips are public content served from local disk —
                        // see FilesystemObjectStorageService / StaticStorageConfig.
                        .requestMatchers(HttpMethod.GET, "/storage/**").permitAll()
                        // Machine auth, not user JWT: SnapshotIngestController verifies the
                        // per-day-instance secret (KTD9) itself, before its own generation check
                        // runs — see its class Javadoc.
                        .requestMatchers(HttpMethod.POST, "/api/v1/localday/events/*/snapshots").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "RACE_DIRECTOR", "REFEREE")
                        .requestMatchers("/ws/timing", "/ws/timing/**").permitAll()
                        // Only officials sign in (L10, #18)
                        .anyRequest().hasAnyRole("ADMIN", "RACE_DIRECTOR", "REFEREE")
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
