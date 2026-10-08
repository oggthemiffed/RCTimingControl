package dev.monkeypatch.rctiming.security;

import dev.monkeypatch.rctiming.config.SpaConfig;
import dev.monkeypatch.rctiming.domain.user.Role;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.util.UrlPathHelper;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] OFFICIAL_ROLES = Role.OFFICIAL_ROLES.stream().map(Role::name).toArray(String[]::new);

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/setup/status").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/setup/bootstrap").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/events", "/api/v1/events/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/results/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/championships/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/about").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/boards/**").permitAll()
                        // Club logos and TTS clips are public content served from local disk —
                        // see FilesystemObjectStorageService / StaticStorageConfig.
                        .requestMatchers(HttpMethod.GET, "/storage/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasAnyRole(OFFICIAL_ROLES)
                        .requestMatchers("/ws/timing", "/ws/timing/**").permitAll()
                        // The bundled frontend (SpaConfig): the pages and their assets are public,
                        // the data behind them is not
                        .requestMatchers(SecurityConfig::isFrontendRequest).permitAll()
                        .anyRequest().hasAnyRole(OFFICIAL_ROLES)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    static boolean isFrontendRequest(HttpServletRequest request) {
        // Decoded, as Spring MVC routes it, so an encoded /api path is not taken for a page
        String path = UrlPathHelper.defaultInstance.getPathWithinApplication(request);
        return (HttpMethod.GET.matches(request.getMethod()) || HttpMethod.HEAD.matches(request.getMethod()))
                && !SpaConfig.isServerPath(path);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
