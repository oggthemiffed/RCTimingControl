package dev.monkeypatch.rctiming.api.auth;

import java.util.List;

public record AuthResponse(
        String accessToken,
        String id,
        String email,
        String firstName,
        String lastName,
        List<String> roles
) {}
