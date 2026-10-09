package dev.monkeypatch.rctiming.api.auth;

import dev.monkeypatch.rctiming.domain.user.User;

import java.util.List;

public record AuthResponse(
        String accessToken,
        String id,
        String email,
        String firstName,
        String lastName,
        List<String> roles
) {

    /** The signed-in official and the access token to send with each request. */
    public static AuthResponse of(User user, String accessToken) {
        return new AuthResponse(
                accessToken,
                user.getId().toString(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRoles().stream().map(Enum::name).toList()
        );
    }
}
