package dev.monkeypatch.rctiming.timing;

/**
 * Pure data record published via ApplicationEventPublisher when a transponder passing is received.
 * The rtcTimeMicros is from the RTC_TIME field of the AMB P3 PASSING record (hardware clock, not server receipt).
 * In Phase 4, SyntheticTimingService fires these events; Phase 5 forwarder replaces that producer.
 */
public record LapPassingEvent(long raceId, String transponderNumber, long rtcTimeMicros) {
}
