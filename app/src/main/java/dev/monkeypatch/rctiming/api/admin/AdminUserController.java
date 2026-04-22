package dev.monkeypatch.rctiming.api.admin;

import dev.monkeypatch.rctiming.api.admin.dto.UserSummaryDto;
import dev.monkeypatch.rctiming.domain.user.UserGoverningBodyMembershipRepository;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR', 'REFEREE')")
public class AdminUserController {

    private final UserRepository userRepository;
    private final UserGoverningBodyMembershipRepository membershipRepository;

    public AdminUserController(UserRepository userRepository,
                                UserGoverningBodyMembershipRepository membershipRepository) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    @GetMapping
    public List<UserSummaryDto> listUsers() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(u -> u.getLastName() + " " + u.getFirstName()))
                .map(u -> UserSummaryDto.from(u, membershipRepository.findByUserId(u.getId())))
                .toList();
    }
}
