package dev.monkeypatch.rctiming.domain.format;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Transactional
public class RaceFormatService {

    private final RaceFormatTemplateRepository templateRepository;
    private final EventClassRepository eventClassRepository;
    private final ObjectMapper objectMapper;

    public RaceFormatService(
            RaceFormatTemplateRepository templateRepository,
            EventClassRepository eventClassRepository,
            ObjectMapper objectMapper) {
        this.templateRepository = templateRepository;
        this.eventClassRepository = eventClassRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Returns the effective config for an EventClass by merging configSnapshot
     * with configOverride. Override values win. If override is null or empty,
     * the snapshot is returned directly.
     */
    public RaceFormatConfig getEffectiveConfig(EventClass eventClass) {
        Map<String, Object> override = eventClass.getConfigOverride();
        if (override == null || override.isEmpty()) {
            return eventClass.getConfigSnapshot();
        }

        try {
            // Serialize snapshot to map
            Map<String, Object> snapshotMap = objectMapper.convertValue(
                    eventClass.getConfigSnapshot(),
                    new TypeReference<Map<String, Object>>() {}
            );

            // Overlay override values (override wins)
            snapshotMap.putAll(override);

            // Deserialize merged map back to RaceFormatConfig
            return objectMapper.convertValue(snapshotMap, RaceFormatConfig.class);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Failed to merge format config override", e);
        }
    }

    /**
     * Creates a new EventClass with a deep copy of the template's config as the
     * configSnapshot. The template reference is stored for audit purposes (FORMAT-06).
     * Template edits do not affect the snapshot after assignment.
     */
    public EventClass assignTemplateToEventClass(RaceFormatTemplate template) {
        // Deep copy: serialize then deserialize to break the reference
        RaceFormatConfig snapshot = objectMapper.convertValue(
                template.getConfig(),
                RaceFormatConfig.class
        );

        EventClass eventClass = new EventClass();
        eventClass.setConfigSnapshot(snapshot);
        eventClass.setTemplate(template);
        return eventClass;
    }
}
