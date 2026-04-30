package dev.monkeypatch.rctiming.api.racer.dto;

import java.util.List;

public record RacerProfileDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        String emergencyContactName,
        String emergencyContactPhone,
        String phoneticName,
        String preferredVoiceId,
        List<MembershipDto> memberships,
        List<ClassRatingDto> classRatings
) {}
