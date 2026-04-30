package dev.monkeypatch.rctiming.infrastructure.tts;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Locale;

@ConfigurationProperties(prefix = "tts")
public record TtsProperties(String endpoint, String defaultVoice, boolean enabled, List<String> locales) {

    /** Effective locale list: configured values, or a single entry derived from the JVM default locale. */
    public List<String> effectiveLocales() {
        if (locales != null && !locales.isEmpty()) {
            return locales;
        }
        Locale jvmLocale = Locale.getDefault();
        String piperLocale = jvmLocale.getLanguage() + "_" + jvmLocale.getCountry().toUpperCase();
        return List.of(piperLocale);
    }
}
