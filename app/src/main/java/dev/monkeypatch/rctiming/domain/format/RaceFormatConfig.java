package dev.monkeypatch.rctiming.domain.format;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = TimedRaceConfig.class,    name = "TIMED"),
    @JsonSubTypes.Type(value = BumpUpConfig.class,       name = "BUMP_UP"),
    @JsonSubTypes.Type(value = PointsFinalsConfig.class, name = "POINTS_FINALS")
})
public sealed interface RaceFormatConfig
    permits TimedRaceConfig, BumpUpConfig, PointsFinalsConfig {}
