# Releases

Each release on the [Releases page](https://github.com/oggthemiffed/RCTimingControl/releases) has installers for Windows (`.msi`), macOS (`.pkg`) and Debian-based Linux (`.deb`); see [docs/installing.md](docs/installing.md). Versions starting with `0.` are pre-releases. The About page shows which version is installed.

---

## Upgrade notes

### The profanity blocklist is gone (#30)

The **Profanity Blocklist** card under **Admin → Audio** is removed, and the upgrade drops any words a club added to it. Nothing had been checked against it since racers stopped typing in their own names (#18).

### The release after 0.1.x: the database moved to SQLite (#26)

The app now keeps its data in a single SQLite file instead of a PostgreSQL server. There is no migration path: an existing PostgreSQL database cannot be upgraded and its data is not carried over.

- Start from an empty database. With Docker, remove the old stack and its volumes (`docker compose -f <compose file> down -v`) before starting the new version.
- The `postgres` and `demo-seed` services and `POSTGRES_PASSWORD` are gone. The database lives in the `app_db` volume (`RCTIMING_DATA_DIR`, default the user's app-data folder outside Docker).
- The trial stack loads its demo club through the app's `demo` profile rather than a seed container.
- Developers: delete any old local database with `make clean-db`.

### Installers and the shared signing key (#23)

- Windows, macOS and Linux installers are attached to each release. See [docs/installing.md](docs/installing.md).
- The app no longer falls back to a signing key written in the source. Without `JWT_SECRET` it creates its own key in the data folder. Docker stacks that relied on the old default get a new key on upgrade, so officials sign in again once.
- Uploads default to an `uploads` folder beside the database, and logos load from the relative `/storage` path. `STORAGE_PUBLIC_BASE_URL` is now optional. A logo uploaded before this release keeps its old absolute URL; upload it again if it does not show on other devices.

### The Docker stacks are gone (#24)

The installers are now the only way to run the app. The production, GHCR and trial Docker stacks, their images and nginx are removed, and the app is not deployed to the internet. To try the app with the demo club, follow [docs/trial-quickstart.md](docs/trial-quickstart.md).

To move a club's data from a Docker stack to an installed copy:

1. In the Docker copy, take a backup under **Admin → Backups**.
2. Copy it out of the container: `docker compose -f <compose file> cp app:/app/data/db/backups/<file> .`
3. Install the new version on the venue laptop, then restore the backup as described in [docs/installing.md](docs/installing.md#restoring-a-backup).

Officials sign in again afterwards, because the installed copy has its own signing key.

