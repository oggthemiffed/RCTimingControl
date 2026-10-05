package dev.monkeypatch.rctiming.domain.user;

/**
 * A change to an official that would lock the club out or clash with another account (#61): the
 * last enabled admin losing ADMIN or being disabled, an admin disabling themselves, or an email
 * already in use. Answered with 409.
 */
public class OfficialChangeRefusedException extends RuntimeException {

    public OfficialChangeRefusedException(String message) {
        super(message);
    }
}
