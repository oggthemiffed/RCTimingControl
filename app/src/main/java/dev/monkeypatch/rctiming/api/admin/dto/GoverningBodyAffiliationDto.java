package dev.monkeypatch.rctiming.api.admin.dto;

import dev.monkeypatch.rctiming.domain.club.GoverningBodyAffiliation;

public record GoverningBodyAffiliationDto(
        Long id,
        String code,
        String displayName,
        boolean membershipRequired) {

    public static GoverningBodyAffiliationDto from(GoverningBodyAffiliation a) {
        return new GoverningBodyAffiliationDto(
                a.getId(),
                a.getCode(),
                a.getDisplayName(),
                a.isMembershipRequired());
    }
}
