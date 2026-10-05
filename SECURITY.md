# Security

## Reporting a vulnerability

Please report security problems privately, using **Report a vulnerability** on the repository's Security tab, rather than in a public issue. Include what you found, how to reproduce it and which version you ran (the About page shows it).

## How the app is meant to run

RC Timing Control runs on one laptop at the track and serves browsers on the venue network. It is not meant to be reachable from the internet, and it serves plain HTTP for that reason. Keep it on a private network.

- **Who signs in.** Only officials (`ADMIN`, `RACE_DIRECTOR`, `REFEREE`) have accounts. Competitors and spectators have none. Passwords are stored as BCrypt hashes.
- **Sessions.** Sign-in issues a short-lived access token and a refresh token in an HttpOnly cookie scoped to the refresh endpoint. See [docs/architecture.md](docs/architecture.md#jwt-authentication).
- **Signing key.** Without `JWT_SECRET`, the app creates its own key in the data folder on first start and restricts the file to the service account on every start. It refuses to start if it cannot. Each installed copy has its own key.
- **Public pages.** The event schedule, live timing, results, championship standings and spectator boards need no sign-in. They show competitor display names, classes, car numbers and timing.
- **Entry data.** RaceHub's Entry Export v1 carries no contact, date of birth, guardian or payment data, so the app holds none.
- **The service.** On Linux the service runs as its own `rctiming` system user with a restricted systemd unit. The database, backups and signing key live in the data folder; see [docs/installing.md](docs/installing.md).
- **The decoder.** The AMB/MyLaps decoder is read over plain TCP on the venue network, as the hardware requires.

Backups contain the whole database, including officials' password hashes. Store copies somewhere only officials can reach.

The security review from the original forwarder phase is kept for history in `.planning/phases/05-live-timing-forwarder/05-SECURITY.md`; the forwarder it covers was removed in #10.
