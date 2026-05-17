package dev.monkeypatch.rctiming.domain.auth;

import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Service
@Transactional
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final String resetBaseUrl;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JavaMailSender mailSender,
            @Value("${app.password-reset.url:http://localhost:5173/reset-password}") String resetBaseUrl) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.resetBaseUrl = resetBaseUrl;
    }

    /**
     * Initiates a password reset for the given email address.
     * Always returns silently — never reveals whether the email exists (email enumeration prevention).
     */
    public void requestReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // Security: never reveal whether email exists
            return;
        }
        User user = userOpt.get();

        // Generate random 32-byte token (hex-encoded = 64 chars)
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String rawToken = HexFormat.of().formatHex(bytes);
        String tokenHash = sha256Hex(rawToken);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(tokenHash);
        resetToken.setExpiresAt(Instant.now().plusSeconds(3600)); // 1 hour
        resetToken.setCreatedAt(Instant.now());
        resetToken.setUsed(false);
        passwordResetTokenRepository.save(resetToken);

        // Send email with reset link
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Password Reset Request");
        message.setText("Reset your password by clicking the link below:\n\n"
                + resetBaseUrl + "?token=" + rawToken
                + "\n\nThis link expires in 1 hour."
                + "\n\nIf you did not request a password reset, please ignore this email.");
        try {
            mailSender.send(message);
        } catch (MailException e) {
            // Log and continue — email delivery failure must not change the HTTP response
            // (security: email enumeration prevention requires identical 200 for all cases)
            log.warn("Failed to send password reset email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    /**
     * Confirms a password reset using the raw token from the email link.
     * Revokes all refresh tokens for the user to force re-login on all devices.
     */
    public void confirmReset(String rawToken, String newPassword) {
        String tokenHash = sha256Hex(rawToken);
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or unknown reset token"));

        if (resetToken.isUsed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset token has already been used");
        }

        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Reset token has expired");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Revoke all refresh tokens — forces re-login on all devices (security: AUTH-03)
        refreshTokenRepository.deleteByUser(user);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
