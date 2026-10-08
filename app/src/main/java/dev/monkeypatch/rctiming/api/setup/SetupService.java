package dev.monkeypatch.rctiming.api.setup;

import dev.monkeypatch.rctiming.domain.StateConflictException;
import dev.monkeypatch.rctiming.api.auth.AuthResponse;
import dev.monkeypatch.rctiming.api.setup.dto.BootstrapRequest;
import dev.monkeypatch.rctiming.api.setup.dto.SetupProgressDto;
import dev.monkeypatch.rctiming.api.setup.dto.SetupStatusDto;
import dev.monkeypatch.rctiming.domain.club.ClubProfileRepository;
import dev.monkeypatch.rctiming.domain.format.RaceFormatTemplateRepository;
import dev.monkeypatch.rctiming.domain.track.TrackRepository;
import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
import dev.monkeypatch.rctiming.domain.user.UserService;
import dev.monkeypatch.rctiming.security.JwtTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SetupService {

    private final ClubProfileRepository clubProfileRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtTokenService jwtTokenService;
    private final TrackRepository trackRepository;
    private final RaceFormatTemplateRepository raceFormatTemplateRepository;

    public SetupService(ClubProfileRepository clubProfileRepository,
                        UserRepository userRepository,
                        UserService userService,
                        JwtTokenService jwtTokenService,
                        TrackRepository trackRepository,
                        RaceFormatTemplateRepository raceFormatTemplateRepository) {
        this.clubProfileRepository = clubProfileRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.jwtTokenService = jwtTokenService;
        this.trackRepository = trackRepository;
        this.raceFormatTemplateRepository = raceFormatTemplateRepository;
    }

    @Transactional(readOnly = true)
    public SetupStatusDto getStatus() {
        boolean bootstrapped = userRepository.count() > 0;
        boolean setupComplete = clubProfileRepository.count() > 0;
        return new SetupStatusDto(bootstrapped, setupComplete);
    }

    @Transactional(readOnly = true)
    public SetupProgressDto getProgress() {
        boolean club = clubProfileRepository.count() > 0;
        boolean track = trackRepository.count() > 0;
        boolean format = raceFormatTemplateRepository.count() > 0;
        // Staff is complete only when a second official exists beyond the bootstrap admin
        boolean staff = userRepository.findAll().stream()
                .filter(User::isOfficial)
                .count() >= 2;
        boolean decoder = clubProfileRepository.findCurrent()
                .map(p -> p.getDecoderHost() != null && p.getDecoderPort() != null && p.getDecoderProtocol() != null)
                .orElse(false);
        return new SetupProgressDto(club, track, format, staff, decoder);
    }

    public AuthResponse bootstrap(BootstrapRequest req) {
        if (userRepository.count() > 0) {
            throw new StateConflictException("Bootstrap already complete");
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
