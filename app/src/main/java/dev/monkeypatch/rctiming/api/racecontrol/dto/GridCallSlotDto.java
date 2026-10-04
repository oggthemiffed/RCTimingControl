package dev.monkeypatch.rctiming.api.racecontrol.dto;

/**
 * A single slot in the grid call list for the upcoming race.
 *
 * @param gridPosition the 1-based grid position; 0 indicates an unseeded bump-up slot with no assigned position
 * @param entryId      the entry ID for this slot
 * @param driverName   the competitor's display name (L5)
 * @param carNumber    the car number/label (null if not recorded in the system)
 * @param className    the racing class name for this event class
 * @param checkedIn      whether the competitor has checked in at the desk (L11)
 * @param racehubArrival RaceHub's arrival mark (NOT_ARRIVED / ARRIVED), read-only; null if not imported
 */
public record GridCallSlotDto(
        int gridPosition,
        long entryId,
        String driverName,
        String carNumber,
        String className,
        boolean checkedIn,
        String racehubArrival
) {}
