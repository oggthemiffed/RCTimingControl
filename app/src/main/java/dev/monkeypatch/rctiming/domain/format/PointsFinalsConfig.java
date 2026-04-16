package dev.monkeypatch.rctiming.domain.format;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PointsFinalsConfig(
    int qualifyingHeats,
    int finalsCount,
    int finalDurationMinutes,
    int heatDurationMinutes,
    StartType qualifyingStartType,
    StartType finalsStartType,
    QualifyingType qualifyingType,
    int racePaddingMinutes,
    int staggerIntervalSeconds
) implements RaceFormatConfig {}
