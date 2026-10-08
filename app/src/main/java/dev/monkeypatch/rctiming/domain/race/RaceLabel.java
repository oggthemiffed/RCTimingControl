package dev.monkeypatch.rctiming.domain.race;

/**
 * How a race is named to people: on the boards, the grid call, result snapshots and the round preview, so
 * they all use the same wording.
 */
public final class RaceLabel {

    private RaceLabel() {
    }

    /** "Qualifying 2 — Stock Buggy — Heat 1", or "A Final — Stock Buggy" for a final. */
    public static String of(String roundType, int roundNumber, String className, int heatNumber, String finalLetter) {
        String round = round(roundType, roundNumber, finalLetter) + " — " + className;
        return RoundType.FINAL.name().equals(roundType) ? round : round + " — Heat " + heatNumber;
    }

    /** "Practice 1", "Qualifying 2" or "B Final"; a final without a letter is the A final. */
    public static String round(String roundType, int roundNumber, String finalLetter) {
        return switch (roundType) {
            case "PRACTICE" -> "Practice " + roundNumber;
            case "QUALIFIER" -> "Qualifying " + roundNumber;
            case "FINAL" -> (finalLetter != null ? finalLetter : "A") + " Final";
            default -> roundType + " " + roundNumber;
        };
    }
}
