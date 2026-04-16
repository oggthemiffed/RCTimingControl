package dev.monkeypatch.rctiming.domain.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RaceFormatServiceTest {

    private RaceFormatService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new RaceFormatService(
                mock(RaceFormatTemplateRepository.class),
                mock(EventClassRepository.class),
                objectMapper
        );
    }

    @Test
    void getEffectiveConfig_withNullOverride_returnsSnapshotUnchanged() {
        TimedRaceConfig snapshot = new TimedRaceConfig(5, StartType.STAGGER, QualifyingType.FTQ, 2, 3);
        EventClass eventClass = new EventClass();
        eventClass.setConfigSnapshot(snapshot);
        eventClass.setConfigOverride(null);

        RaceFormatConfig effective = service.getEffectiveConfig(eventClass);

        assertThat(effective).isEqualTo(snapshot);
    }

    @Test
    void getEffectiveConfig_withOverride_appliesOverridedField() throws Exception {
        TimedRaceConfig snapshot = new TimedRaceConfig(10, StartType.STAGGER, QualifyingType.FTQ, 2, 3);
        EventClass eventClass = new EventClass();
        eventClass.setConfigSnapshot(snapshot);
        eventClass.setConfigOverride(Map.of("durationMinutes", 15));

        RaceFormatConfig effective = service.getEffectiveConfig(eventClass);

        assertThat(effective).isInstanceOf(TimedRaceConfig.class);
        TimedRaceConfig result = (TimedRaceConfig) effective;
        assertThat(result.durationMinutes()).isEqualTo(15);
        assertThat(result.startType()).isEqualTo(StartType.STAGGER);
        assertThat(result.qualifyingType()).isEqualTo(QualifyingType.FTQ);
        assertThat(result.racePaddingMinutes()).isEqualTo(2);
    }

    @Test
    void getEffectiveConfig_withUnknownOverrideField_isIgnored() throws Exception {
        TimedRaceConfig snapshot = new TimedRaceConfig(5, StartType.GRID, QualifyingType.FTQ, 2, 0);
        EventClass eventClass = new EventClass();
        eventClass.setConfigSnapshot(snapshot);
        eventClass.setConfigOverride(Map.of("unknownFutureField", "someValue"));

        RaceFormatConfig effective = service.getEffectiveConfig(eventClass);

        assertThat(effective).isInstanceOf(TimedRaceConfig.class);
        TimedRaceConfig result = (TimedRaceConfig) effective;
        assertThat(result.durationMinutes()).isEqualTo(5);
    }

    @Test
    void assignTemplateToEventClass_createsDeepCopy_notReference() throws Exception {
        TimedRaceConfig originalConfig = new TimedRaceConfig(5, StartType.STAGGER, QualifyingType.FTQ, 2, 3);
        RaceFormatTemplate template = new RaceFormatTemplate();
        template.setName("5-minute timed");
        template.setConfig(originalConfig);

        EventClass eventClass = service.assignTemplateToEventClass(template);

        assertThat(eventClass.getConfigSnapshot()).isEqualTo(originalConfig);
        assertThat(eventClass.getConfigSnapshot()).isNotSameAs(originalConfig);
        assertThat(eventClass.getTemplate()).isSameAs(template);
    }
}
