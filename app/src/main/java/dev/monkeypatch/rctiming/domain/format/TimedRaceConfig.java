package dev.monkeypatch.rctiming.domain.format;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TimedRaceConfig(
    int durationMinutes,
    StartType startType,
    QualifyingType qualifyingType,
    int racePaddingMinutes,
    int staggerIntervalSeconds
) implements RaceFormatConfig {}
