-- V23: Phase 6 Audio & Practice schema additions

-- 1. PracticeSession table (PRACTICE-01)
CREATE TABLE practice_sessions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    event_id BIGINT REFERENCES events(id),          -- nullable: standalone or event-linked
    status VARCHAR(20) NOT NULL DEFAULT 'IDLE',      -- IDLE, RUNNING, STOPPED
    best_lap_n INT NOT NULL DEFAULT 3,               -- configurable consecutive-lap count
    created_by_user_id BIGINT REFERENCES users(id),
    started_at TIMESTAMPTZ,
    stopped_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_practice_sessions_event ON practice_sessions(event_id)
    WHERE event_id IS NOT NULL;

-- 2. PracticeLap table (for recording practice lap data)
CREATE TABLE practice_laps (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    practice_session_id BIGINT NOT NULL REFERENCES practice_sessions(id),
    transponder_number VARCHAR(50) NOT NULL,
    user_id BIGINT REFERENCES users(id),            -- nullable until linked
    lap_number INT NOT NULL,
    lap_time_ms BIGINT NOT NULL,
    crossing_time TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_practice_laps_session ON practice_laps(practice_session_id);
CREATE INDEX idx_practice_laps_transponder ON practice_laps(transponder_number);

-- 3. User audio preferences (AUDIO-13)
ALTER TABLE users
    ADD COLUMN preferred_voice_id VARCHAR(100);

-- 4. Club audio settings (AUDIO-07) — JSONB column on club_profiles
ALTER TABLE club_profiles
    ADD COLUMN audio_settings JSONB NOT NULL DEFAULT '{
        "announceCountdown": true,
        "announceStagger": true,
        "announceLapBeep": true,
        "announceFinish": true,
        "announceRunningOrder": true,
        "runningOrderDepth": 3
    }'::jsonb,
    ADD COLUMN default_voice_id VARCHAR(100) NOT NULL DEFAULT 'en_GB-alan-medium';

-- 5. Profanity blocklist (AUDIO-14)
CREATE TABLE profanity_blocklist (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    word VARCHAR(200) NOT NULL UNIQUE,
    added_by_user_id BIGINT REFERENCES users(id),
    added_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_profanity_word_lower ON profanity_blocklist(LOWER(word));
