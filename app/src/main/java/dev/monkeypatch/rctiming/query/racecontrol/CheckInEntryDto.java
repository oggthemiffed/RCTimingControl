package dev.monkeypatch.rctiming.query.racecontrol;

import java.time.Instant;

/**
 * One entry as the check-in desk sees it (L11).
 *
 * @param competitorId the competitor, so the desk can fix how their name is said (#119)
 * @param spokenName   the admin's override for how the name is said aloud, or null for none
 * @param speechName   what the announcer says for them: the spoken name, else the display name tidied (#120)
 * @param racehubArrival RaceHub's arrival mark (NOT_ARRIVED / ARRIVED), read-only; null for
 *                       entries not imported from RaceHub
 * @param importedTransponderNumber the imported file's primary, when it differs from a number swapped on the
 *                                  day (#50); null otherwise
 * @param importedSecondaryTransponderNumber the same for the secondary
 */
public record CheckInEntryDto(
        long entryId,
        String competitorName,
        Long competitorId,
        String spokenName,
        String speechName,
        String className,
        String transponderNumber,
        String secondaryTransponderNumber,
        boolean checkedIn,
        Instant checkedInAt,
        String racehubArrival,
        String importedTransponderNumber,
        String importedSecondaryTransponderNumber
) {}
