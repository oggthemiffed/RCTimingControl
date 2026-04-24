package dev.monkeypatch.rctiming.api.racecontrol.dto;

/**
 * A row in the marshal duty list — one driver from the previous race, annotated with
 * the number of times they failed to marshal at any race in this event (D-21).
 *
 * @param entryId         the entry ID for this driver
 * @param driverName      the driver's display name (first + last name, or email fallback)
 * @param carNumber       the car number/label (null if not recorded in the system)
 * @param missedThisEvent count of marshal_absences records for this entry in this event
 */
public record MarshalDutyRowDto(
        long entryId,
        String driverName,
        String carNumber,
        long missedThisEvent
) {}
