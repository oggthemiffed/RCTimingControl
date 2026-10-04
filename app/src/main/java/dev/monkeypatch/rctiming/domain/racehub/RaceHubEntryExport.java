package dev.monkeypatch.rctiming.domain.racehub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

/**
 * RaceHub Entry Export v1 (L7, #15). Only the fields the export carries are read. Anything else
 * in the document, such as a field added in a later minor version, is ignored, so contact,
 * date of birth, guardian or payment data can never be accepted.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RaceHubEntryExport(
        Integer schemaVersion,
        ExportEvent event,
        Long revision,
        List<ExportEntry> entries) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ExportEvent(
            String id,
            String externalReference,
            String name,
            String eventLocalDate,
            String timezone) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ExportEntry(
            String entryId,
            Long entryVersion,
            String entryStatus,
            String raceDayStatus,
            String eventClassId,
            String rcClassName,
            String rcClassNumber,
            String className,
            String driverProfileId,
            String driverDisplayName,
            String brcaNumber,
            String brcaSource,
            String homeClub,
            String ageCategory,
            String grade,
            String primaryTransponder,
            String secondaryTransponder,
            Boolean clubCarRequested,
            String clubCarName) {
    }
}
