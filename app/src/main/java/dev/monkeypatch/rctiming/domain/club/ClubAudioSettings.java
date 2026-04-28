package dev.monkeypatch.rctiming.domain.club;

public record ClubAudioSettings(
        boolean announceCountdown,
        boolean announceStagger,
        boolean announceLapBeep,
        boolean announceFinish,
        boolean announceRunningOrder,
        int runningOrderDepth
) {
    public static ClubAudioSettings defaults() {
        return new ClubAudioSettings(true, true, true, true, true, 3);
    }
}
