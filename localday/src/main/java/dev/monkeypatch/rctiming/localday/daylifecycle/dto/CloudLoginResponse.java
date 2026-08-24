package dev.monkeypatch.rctiming.localday.daylifecycle.dto;

import java.util.List;

/** Inbound body from the cloud's {@code POST /api/v1/auth/login} 200 response. */
public record CloudLoginResponse(String accessToken, String id, String email,
                                  String firstName, String lastName, List<String> roles) {}
