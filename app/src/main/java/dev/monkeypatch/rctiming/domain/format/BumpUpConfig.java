package dev.monkeypatch.rctiming.domain.format;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BumpUpConfig(
    int qualifyingHeats,
    int heatDurationMinutes,
    int bestHeatsCount,
    int gridSize,
    int bumpSpots,
    StartType qualifyingStartType,
    StartType finalsStartType,
    QualifyingType qualifyingType,
    int racePaddingMinutes,
    int staggerIntervalSeconds
) implements RaceFormatConfig {}
