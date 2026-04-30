# Phase 6 — Human UAT Checklist

**Phase:** 06-audio-practice
**Verified by automated tests:** 82 tests (52 backend + 30 frontend) — all passing
**Requires human verification:** Audio output, live decoder hardware

---

## Prerequisites

- App running with Docker (`docker-compose up` — Postgres + MinIO + Piper TTS)
- Logged in as a user with **ADMIN** + **RACE_DIRECTOR** roles
- For decoder-dependent tests: live AMB decoder or fake decoder from Phase 5

---

## 🔊 AUDIO — TTS & Voice Setup

- [ ] **[AUDIO-08/13]** Go to **Racer Profile → "Announcement Voice"** section
  - Voice dropdown populates from `/api/v1/audio/voices`
  - Selecting a voice and clicking **Preview** plays audible speech in the browser
  - Saving preferences persists across page reload

**Results** -> Fail - can select any voices for the TTS so not able to actually test, seems like nothing work in this part of the racers profile :(

- [ ] **[AUDIO-14]** Go to **Admin → Audio Settings → Profanity Blocklist**
  - Add a custom word → it appears in the list
  - Delete it → it disappears
  - Try saving a racer display name containing a blocked word → save is rejected with an error

**Results** -> Fail - Unable to add a word to the block list, now have a 500 on save (java.lang.NullPointerException: Cannot invoke "org.springframework.security.core.userdetails.UserDetails.getUsername()" because "userDetails" is null)

- [/] **[AUDIO-07]** Go to **Admin → Audio Settings**
  - 5 announcement toggles display correctly (Countdown, Stagger, Lap Beep, Finish, Running Order)
  - Default voice selector populates and saves
  - Saving settings persists across page reload

- [/] **[AUDIO-11]** Go to **Race Control → CockpitPage → Audio Settings panel** (sidebar)
  - Panel expands and collapses
  - 5 toggle switches visible
  - Volume slider adjusts (0–100%)
  - **"▶ Test Audio"** button produces audible speech in the browser
  - Status dot is green when any toggle is on, red when all are off

---

## 🏁 AUDIO — Live Race Announcements *(requires live or fake decoder)*

- [/] **[AUDIO-04]** With a race RUNNING and transponders crossing:
  - A **high-pitched beep** fires when a racer beats their best lap
  - A **low-pitched beep** fires on a slower lap
  - Toggling **"Lap Beep"** off silences it

- [/] **[AUDIO-06]** With a race RUNNING:
  - Wait for the running-order interval (or shorten `countdownIntervals` in config)
  - Browser **announces the running order aloud** (e.g. "Running order: 1st, Car 7. 2nd, Car 12…")
  - Toggling **"Running Order"** off silences it

- [/] **[AUDIO-02/AUDIO-05]** Transition a race to **GRID**:
  - Browser Network tab shows a request to `/api/v1/race/{id}/audio-clips` (clip map fetch)
  - At configured countdown intervals, browser announces countdown (e.g. "30 seconds remaining")
  - On race **FINISH**, browser announces "Race finished. Checkered flag."
  - Toggling **"Countdown"** / **"Finish"** off silences the respective announcement


- [/] **[AUDIO-03]** In stagger mode, when race director calls running order:
  - Car-number announcements fire sequentially, ~2 seconds apart
  - Toggling **"Stagger"** off silences them

---

## 🏎️ PRACTICE — Session Management *(requires live or fake decoder)*

**OVERALL RESULTS** - there is no practice funcationality :( all failed

- [/] **[PRACTICE-01]** Navigate to **Race Control → Practice**
  - Empty IDLE state shown with "Start Practice" CTA
  - Click **New Session** → dialog opens with Name field and "Best N laps" selector (default 3)
  - Create a session → session appears as IDLE

- [/] **[PRACTICE-01]** Click **Start** → session transitions to RUNNING
  - With transponder crossings incoming, the **live table updates in real time**
  - Columns visible: Position, Name/Transponder, Laps, Best Lap, Best N Consec, Last Lap
  - Lap times formatted as `m:ss.mmm`

- [ ] **[PRACTICE-02]** With ≥ 3 laps recorded per racer:
  - **"Best N Consec."** column shows the minimum-sum window of N consecutive laps
  - Fastest racer over that window is ranked first

- [ ] **[PRACTICE-01]** Unknown transponder (not linked to any entry):
  - **Yellow banner** appears at the top of the practice page with the transponder number
  - "Link to entry" button is present

- [/] **[PRACTICE-01]** Click **Stop** → session transitions to STOPPED
  - Final snapshot is preserved in the table
  - **Print Results** button appears

- [ ] **[PRACTICE-01]** Click **Print Results** → opens print-optimised page
  - Shows session name, date, and results table
  - Browser print dialog works correctly
**Results** nothing was able to be printed or shown, had an error 'Could not load results.' on screen

**comments** could we have the practice screen use the same row highlights are the other screens?
---

## 👤 ADMIN — Racer Phonetic Name

**Overall Fail** - can see the racer list but get this messgae when trying to examine one: Error: Too many re-renders. React limits the number of renders to prevent an infinite loop.

- [ ] **[AUDIO-15]** Go to **Admin → Racers → [any racer]**
  - **Phonetic Name** field is visible
  - Save a phonetic override → check MinIO (or app logs) that a TTS clip was regenerated
  - Clear the phonetic name → reverts to display name for TTS

---

## Summary

**feedback** it appears that after a clean of the db and source, we have nothing in the drop down lists to choose a vioce type, can you have a look and fix this and ensure it is properly seeded or setup

| Area | Items | Requires Hardware |
|------|-------|-------------------|
| TTS & Voice Setup | 4 | No (Piper via Docker) |
| Live Race Announcements | 4 | Yes (decoder) |
| Practice Session | 6 | Yes (decoder) |
| Admin Phonetic Name | 1 | No |
| **Total** | **15** | |
