package dev.monkeypatch.rctiming.api.setup;

import dev.monkeypatch.rctiming.api.auth.AuthResponse;
import dev.monkeypatch.rctiming.api.setup.dto.BootstrapRequest;
import dev.monkeypatch.rctiming.api.setup.dto.SetupProgressDto;
import dev.monkeypatch.rctiming.api.setup.dto.SetupStatusDto;
import dev.monkeypatch.rctiming.domain.club.ClubProfileRepository;
import dev.monkeypatch.rctiming.domain.format.RaceFormatTemplateRepository;
import dev.monkeypatch.rctiming.domain.track.TrackRepository;
import dev.monkeypatch.rctiming.domain.user.Role;
import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
import dev.monkeypatch.rctiming.domain.user.UserService;
import dev.monkeypatch.rctiming.forwarder.ForwarderTokenService;
import dev.monkeypatch.rctiming.security.JwtTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Instant;

@Service
@Transactional
public class SetupService {

    private final ClubProfileRepository clubProfileRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtTokenService jwtTokenService;
    private final TrackRepository trackRepository;
    private final RaceFormatTemplateRepository raceFormatTemplateRepository;
    private final ForwarderTokenService forwarderTokenService;

    public SetupService(ClubProfileRepository clubProfileRepository,
                        UserRepository userRepository,
                        UserService userService,
                        JwtTokenService jwtTokenService,
                        TrackRepository trackRepository,
                        RaceFormatTemplateRepository raceFormatTemplateRepository,
                        ForwarderTokenService forwarderTokenService) {
        this.clubProfileRepository = clubProfileRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.jwtTokenService = jwtTokenService;
        this.trackRepository = trackRepository;
        this.raceFormatTemplateRepository = raceFormatTemplateRepository;
        this.forwarderTokenService = forwarderTokenService;
    }

    @Transactional(readOnly = true)
    public SetupStatusDto getStatus() {
        return new SetupStatusDto(clubProfileRepository.count() > 0);
    }

    @Transactional(readOnly = true)
    public SetupProgressDto getProgress() {
        boolean club = clubProfileRepository.count() > 0;
        boolean track = trackRepository.count() > 0;
        boolean format = raceFormatTemplateRepository.count() > 0;
        // RESEARCH.md A3: user table is tiny in a club deployment; findAll() stream is acceptable
        boolean staff = userRepository.findAll().stream()
                .anyMatch(u -> u.getRoles().stream().anyMatch(r -> r != Role.RACER));
        boolean decoder = clubProfileRepository.findAll().stream().findFirst()
                .map(p -> p.getDecoderHost() != null && p.getDecoderPort() != null && p.getDecoderProtocol() != null)
                .orElse(false);
        return new SetupProgressDto(club, track, format, staff, decoder);
    }

    @Transactional(readOnly = true)
    public String generateForwarderEnv(HttpServletRequest request) {
        // T-08-03: plaintext token is NOT stored — env file carries only a placeholder
        var profile = clubProfileRepository.findAll().stream().findFirst().orElse(null);
        var status = forwarderTokenService.getCurrentStatus();
        String baseUrl = ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath("")
                .build()
                .toUriString();
        StringBuilder sb = new StringBuilder();
        sb.append("# forwarder.env — generated ").append(Instant.now()).append(" by RC Timing setup wizard\n");
        sb.append("# Token status: ").append(status.status() == null ? "NONE" : status.status())
          .append(", generated ").append(status.generatedAt() == null ? "never" : status.generatedAt()).append("\n");
        sb.append("# IMPORTANT: paste the plaintext token shown when you generated it.\n");
        sb.append("# The server cannot recover plaintext tokens (T-08-03).\n\n");
        sb.append("APP_SERVER_URL=").append(baseUrl).append("\n");
        sb.append("APP_FORWARDER_TOKEN=<paste-your-token-here>\n");
        sb.append("APP_DECODER_HOST=").append(profile == null || profile.getDecoderHost() == null ? "" : profile.getDecoderHost()).append("\n");
        sb.append("APP_DECODER_PORT=").append(profile == null || profile.getDecoderPort() == null ? "" : profile.getDecoderPort()).append("\n");
        sb.append("APP_DECODER_PROTOCOL=").append(profile == null || profile.getDecoderProtocol() == null ? "" : profile.getDecoderProtocol()).append("\n");
        return sb.toString();
    }

    public AuthResponse bootstrap(BootstrapRequest req) {
        if (userRepository.count() > 0) {
            throw new IllegalStateException("Bootstrap already complete");
        }
        User user = userService.createAdmin(req.email(), req.password(), req.firstName(), req.lastName());
        String token = jwtTokenService.generateAccessToken(user);
        return new AuthResponse(
                token,
                user.getId().toString(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRoles().stream().map(Enum::name).toList()
        );
    }
}
