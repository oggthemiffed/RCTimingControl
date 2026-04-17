package dev.monkeypatch.rctiming.api.racer.dto;

import dev.monkeypatch.rctiming.domain.user.UserGoverningBodyMembership;

public record MembershipDto(String governingBodyCode, String membershipNumber) {

    public static MembershipDto from(UserGoverningBodyMembership m) {
        return new MembershipDto(m.getGoverningBodyCode(), m.getMembershipNumber());
    }
}
