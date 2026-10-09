package dev.monkeypatch.rctiming.security;

import dev.monkeypatch.rctiming.domain.audit.AuditService;
import dev.monkeypatch.rctiming.domain.auth.RefreshTokenRepository;
import dev.monkeypatch.rctiming.domain.user.UserService;
import dev.monkeypatch.rctiming.security.AuthService.Outcome;
import dev.monkeypatch.rctiming.security.AuthService.Refusal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserService userService;
    @Mock JwtTokenService jwtTokenService;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) AuditService audit;
    @Mock TransactionTemplate transactions;

    @Test
    void login_withAnUnknownEmail_stillChecksAPassword_soItIsNoQuickerToRefuse() {
        when(passwordEncoder.encode(any())).thenReturn("unknown-email-hash");
        AuthService service = new AuthService(userService, jwtTokenService, refreshTokenRepository, passwordEncoder,
                audit, transactions);
        when(userService.findByEmail("nobody@club.test")).thenReturn(Optional.empty());

        Outcome outcome = service.login("nobody@club.test", "guess");

        assertThat(outcome).isEqualTo(new Outcome.Refused(Refusal.WRONG_CREDENTIALS));
        verify(passwordEncoder).matches(eq("guess"), eq("unknown-email-hash"));
        verifyNoInteractions(refreshTokenRepository, transactions);
    }
}
