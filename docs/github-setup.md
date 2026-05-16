# GitHub Repository Setup

One-time configuration needed before Docker images can be published to GitHub Container Registry (GHCR) and pulled by non-technical users.

---

## Step 1 — Allow Actions to write packages

Go to:
```
https://github.com/oggthemiffed/RCTimingControl/settings/actions
```

Under **Workflow permissions**, select **"Read and write permissions"** and click Save.

Without this, the `GITHUB_TOKEN` cannot push images to GHCR even though the workflow requests `packages: write`.

---

## Step 2 — Push the first release tag

This creates the GHCR package entries so you can configure their visibility.

```bash
git tag v0.1.0
git push origin v0.1.0
```

Wait for the **Publish Trial Images** workflow to complete:
```
https://github.com/oggthemiffed/RCTimingControl/actions/workflows/publish-trial-images.yml
```

This builds and pushes 5 images (`app`, `frontend`, `forwarder`, `fake-decoder`, `seed`) and creates a GitHub Release with `docker-compose.ghcr.yml` and `.env.example` attached.

---

## Step 3 — Make each package public

By default GHCR packages are **private**. Non-technical users running `docker compose up` will get an authentication error unless the images are public.

Go to each of these URLs and click **"Change visibility" → Public**:

| Image | Settings URL |
|-------|-------------|
| app | https://github.com/users/oggthemiffed/packages/container/rctimingcontrol%2Fapp/settings |
| frontend | https://github.com/users/oggthemiffed/packages/container/rctimingcontrol%2Ffrontend/settings |
| forwarder | https://github.com/users/oggthemiffed/packages/container/rctimingcontrol%2Fforwarder/settings |
| fake-decoder | https://github.com/users/oggthemiffed/packages/container/rctimingcontrol%2Ffake-decoder/settings |
| seed | https://github.com/users/oggthemiffed/packages/container/rctimingcontrol%2Fseed/settings |

You only need to do this once — the visibility setting persists for all future pushes to that package name.

---

## Step 4 — Verify

```bash
# Pull one image to confirm it's publicly accessible (no docker login needed)
docker pull ghcr.io/oggthemiffed/rctimingcontrol/app:0.1.0
```

Then test the full trial stack:
```bash
cp .env.example .env
# .env already has RCTIMING_VERSION=v0.1.0
docker compose -f docker-compose.ghcr.yml up
```

Open **http://localhost** — the About page should show `v0.1.0`.

---

## Done — future releases

For all subsequent releases you only need to:

```bash
echo "0.2.0" > VERSION
# update RCTIMING_VERSION=v0.2.0 in .env.example
git add VERSION .env.example
git commit -m "chore: bump version to 0.2.0"
git push origin master
git tag v0.2.0
git push origin v0.2.0
```

CI does the rest. See [RELEASES.md](../RELEASES.md) for the full release checklist.
