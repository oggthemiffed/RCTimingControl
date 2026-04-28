package dev.monkeypatch.rctiming.domain.club;

public record ClubAudioSettings(
        boolean announceCountdown,
        boolean announceStagger,
        boolean announceLapBeep,
        boolean announceFinish,
        boolean announceRunningOrder,
        int runningOrderDepth,
        int[] countdownIntervals
) {
    /**
     * Compact constructor — ensures countdownIntervals is never null.
     * Default sequence: 10 min, 5 min, 2 min, 1 min, 30 sec (in seconds).
     */
    public ClubAudioSettings {
        if (countdownIntervals == null) {
            countdownIntervals = new int[]{600, 300, 120, 60, 30};
        }
    }

    public static ClubAudioSettings defaults() {
        return new ClubAudioSettings(true, true, true, true, true, 3, new int[]{600, 300, 120, 60, 30});
    }
}

