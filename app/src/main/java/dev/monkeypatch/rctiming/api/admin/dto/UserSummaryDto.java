package dev.monkeypatch.rctiming.api.admin.dto;

import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserGoverningBodyMembership;

import java.util.List;

public record UserSummaryDto(Long id, String firstName, String lastName, List<MembershipRef> memberships) {

    public record MembershipRef(String code, String number) {}

    public static UserSummaryDto from(User user, List<UserGoverningBodyMembership> memberships) {
        return new UserSummaryDto(
                user.getId(), user.getFirstName(), user.getLastName(),
                memberships.stream()
                        .map(m -> new MembershipRef(m.getGoverningBodyCode(), m.getMembershipNumber()))
                        .toList());
    }

    public String displayName() {
        return firstName + " " + lastName;
    }
}
