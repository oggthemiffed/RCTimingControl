package dev.monkeypatch.rctiming.security;

import dev.monkeypatch.rctiming.domain.audit.Actor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentOfficialTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void aSignedInOfficial_isReadFromTheTokenSubject() {
        var auth = new UsernamePasswordAuthenticationToken("42", null, List.of());

        assertThat(CurrentOfficial.id(auth)).isEqualTo(42L);
        assertThat(CurrentOfficial.actor(auth)).isEqualTo(Actor.official(42L));
    }

    @Test
    void withoutAnAuthentication_readsTheSecurityContext() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("7", null, List.of()));

        assertThat(CurrentOfficial.id()).isEqualTo(7L);
        assertThat(CurrentOfficial.actor()).isEqualTo(Actor.official(7L));
    }

    @Test
    void noOfficial_isRefused() {
        assertThatThrownBy(CurrentOfficial::id).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        var anonymous = new AnonymousAuthenticationToken("key", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        assertThatThrownBy(() -> CurrentOfficial.id(anonymous))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }
}
