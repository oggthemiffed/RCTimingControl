package dev.monkeypatch.rctiming.domain.user;

import java.time.Instant;

/**
 * An official's sessions were ended (#61): they were disabled or given a new password. Their refresh
 * tokens are already revoked; listeners end anything else they hold open, such as live timing sockets.
 */
public record OfficialSignedOutEvent(long userId, Instant at) {}
