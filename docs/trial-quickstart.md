# RCTimingControl Trial — Quickstart Guide

> **Early pre-release (v0.1)** — This is a preview build for club evaluation. Features are complete but the software has not been through a full race meeting yet. Please report anything that doesn't work.

This guide gets you from zero to a fully running system in about five minutes. You do **not** need to install Java, Node, or any developer tools — just Docker.

---

## What you get

- Full race management system pre-loaded with a demo club (Wyvern RC Club)
- Eight demo racer accounts with cars, transponders, and a completed historical event
- A fake AMB decoder running in the background, sending live lap data so the race control display is active
- Every feature available to explore: racer portal, race control, admin panel, championship standings

---

## Step 1 — Install Docker Desktop

If you don't have Docker already:

- **Windows / Mac:** Download from [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop/) and run the installer. Accept all defaults.
- **Linux:** Follow the instructions for your distribution at [docs.docker.com/engine/install](https://docs.docker.com/engine/install/).

Once installed, open Docker Desktop and make sure it says **"Docker Desktop is running"** before continuing.

---

## Step 2 — Download the trial files

Go to the [latest release page](https://github.com/oggthemiffed/RCTimingControl/releases/latest) and download the two files attached to the release:

| File | What it is |
|------|-----------|
| `docker-compose.ghcr.yml` | Tells Docker which services to start |
| `.env.example` | Configuration template |

Save both into a new folder on your computer (e.g. `rctiming-trial` on your Desktop), then **rename** `.env.example` to `.env` (remove the `.example` part).

---

## Step 3 — Start the system

Open a terminal (Command Prompt or PowerShell on Windows, Terminal on Mac/Linux), navigate to your `rctiming-trial` folder, and run:

```bash
docker compose -f docker-compose.ghcr.yml up
```

The first run downloads the images — this takes a few minutes depending on your internet connection. You'll see a lot of log output. When you see lines like:

```
app    | Started RcTimingControlApplication in ...
```

the system is ready.

---

## Step 4 — Open the app

Go to **[http://localhost](http://localhost)** in your browser.

---

## Demo accounts

All accounts use the password: **`trial123`**

| Email | Role | What you can do |
|-------|------|-----------------|
| `admin@example.com` | Admin / Race Director / Referee | Everything — admin panel, race control, referee tools |
| `dave.racer@example.com` | Racer | Racer portal — profile, cars, transponders, entries |
| `sam.speed@example.com` | Racer | Same as above |
| `jo.turner@example.com` | Racer | Same as above |

---

## Things to try

### As a racer (`dave.racer@example.com`)

1. Log in and view your profile, car, and transponder
2. Look at the event schedule — the Wyvern Winter Series Round 4 is listed as an upcoming open event
3. View your entry history and past results

### As admin (`admin@example.com`)

1. Go to **Admin** → **Events** to see the championship event and its races
2. Go to **Admin** → **Championships** to see the points standings
3. Go to **Admin** → **Club Profile** to see the club profile (you can edit this to match your own club)
4. Go to **Admin** → **Forwarder Token** to see how the decoder connection is configured

### Race control

1. Log in as `admin@example.com`
2. Go to **Race Control** from the top navigation
3. The fake decoder is sending live lap passings — you should see timing data updating in real time
4. Try starting a race: select a race, click **Call Grid**, then **Start**

### Event schedule (public)

Open [http://localhost/events](http://localhost/events) in a second browser tab — this is the public-facing event schedule, no login required.

---

## Stopping the system

Press `Ctrl+C` in the terminal where Docker is running, then:

```bash
docker compose -f docker-compose.ghcr.yml down
```

Your data is preserved between restarts. To wipe everything and start fresh:

```bash
docker compose -f docker-compose.ghcr.yml down -v
```

---

## Troubleshooting

**Port 80 is already in use**

Edit `.env` and change `HOST_PORT=80` to `HOST_PORT=8080` (or any free port), then restart. Access the app at `http://localhost:8080`.

**Services keep restarting / app won't start**

Wait a bit longer — PostgreSQL takes 10–20 seconds to initialise on first boot and the app will retry automatically.

**"Set RCTIMING_VERSION in .env" error**

Make sure you renamed `.env.example` to `.env` (not `.env.example`).

**Everything looks blank / no demo data**

The `demo-seed` container runs once on first boot. Check it completed: `docker compose -f docker-compose.ghcr.yml ps` — the `demo-seed` row should show `Exited (0)`. If it shows a non-zero exit code, run `docker compose -f docker-compose.ghcr.yml logs demo-seed` to see what went wrong.

---

## Feedback

This is an early pre-release (v0.1). If something doesn't work or a feature is missing, please let us know — that's exactly what this trial is for.
