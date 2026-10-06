# Running with Docker

Docker runs the same app on Windows, macOS and Linux. It's the quickest way to try the demo club, because it needs no installer and no release.

## What you need

- **Windows or macOS:** [Docker Desktop](https://www.docker.com/products/docker-desktop/), installed and running.
- **Linux:** Docker Engine with the Compose plugin (`docker compose version` should print a version).
- A copy of this repository: `git clone https://github.com/oggthemiffed/RCTimingControl.git`, or **Code → Download ZIP** on GitHub, unzipped.

The first start builds the app inside Docker, which takes several minutes. Later starts reuse the build.

## Try the demo

In a terminal, in the repository folder, run:

```bash
docker compose -f docker-compose.demo.yml up --build
```

This starts the app with the demo club (Wyvern RC Club) and a simulated decoder that sends laps for the demo club's eight transponders. When the log shows `Connected to RC-4 decoder at localhost:5100`, open **[http://localhost:8080](http://localhost:8080)** and sign in as `admin@example.com` with the password **`trial123`**. That account is an admin, race director and referee.

Then follow [Things to try](trial-quickstart.md#things-to-try) in the trial guide to run a race with live timing.

Press `Ctrl+C` to stop the demo. Run the same `up` command (without `--build`) to start it again with the same data.

### Starting again from scratch

To delete the demo's data and start with a fresh demo club:

```bash
docker compose -f docker-compose.demo.yml down -v
```

After pulling a newer version of the repository, add `--build` to the `up` command so Docker rebuilds the app.

### Opening it on other devices

Phones, tablets and other computers on the same network can use the demo too, at the address of the computer running Docker, for example `http://192.168.1.50:8080/`. The About page lists the container's own addresses, which other devices can't reach, so use the computer's address instead. On Windows and macOS, allow Docker Desktop through the firewall if a phone can't connect.

### If port 8080 is in use

Set a different port when starting:

```bash
HOST_PORT=8081 docker compose -f docker-compose.demo.yml up --build
```

On Windows Command Prompt, run `set HOST_PORT=8081` first. Then open `http://localhost:8081`.

### Voice announcements

The demo has no voice server, so race control uses the browser's own voice for announcements.

## Running a club with Docker

The same image can run a real club's app. Leave out the demo profile and the simulator, keep the data in a volume, and set the decoder's address in the setup wizard or **Admin → Decoder**. A minimal `docker-compose.yml` for this:

```yaml
services:
  app:
    build:
      context: .
      dockerfile: docker/Dockerfile
    restart: unless-stopped
    environment:
      TTS_ENABLED: "false"
    ports:
      - "8080:8080"
    volumes:
      - club_data:/data

volumes:
  club_data:
```

Everything the app keeps is in `/data` inside the container: the database `rctiming.db`, `backups/`, `uploads/` and the `jwt-secret` sign-in key. Settings that [installing.md](installing.md#changing-settings) puts in `application.properties` can be given as environment variables instead, for example `RCTIMING_BACKUP_DIRECTORY`.

To restore a backup, stop the app and run the restore command in a one-off container:

```bash
docker compose stop app
docker compose run --rm app restore /data/backups/<file>
docker compose start app
```

`reset-admin-password` works the same way (see [installing.md](installing.md#locked-out)).

The app listens on plain HTTP and is meant for the venue network. Don't expose it to the internet as it is.
