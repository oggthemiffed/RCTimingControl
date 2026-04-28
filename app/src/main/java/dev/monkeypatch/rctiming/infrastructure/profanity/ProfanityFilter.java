package dev.monkeypatch.rctiming.infrastructure.profanity;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Checks input text against a profanity blocklist.
 * <p>
 * Loads base terms from {@code profanity/base-words.txt} on the classpath, then
 * merges club-specific terms from the {@code profanity_blocklist} database table.
 * Matching is case-insensitive word-boundary regex.
 * <p>
 * Call {@link #reload()} after admin adds or removes custom terms.
 */
@Component
public class ProfanityFilter {

    private static final Logger log = LoggerFactory.getLogger(ProfanityFilter.class);
    private static final String BASE_WORDS_PATH = "profanity/base-words.txt";

    private final ProfanityBlocklistRepository blocklistRepository;

    /** Current set of blocked words (lowercase). */
    private volatile Set<String> blockedWords = Set.of();
    /** Compiled pattern for fast matching. */
    private volatile Pattern blockPattern = Pattern.compile("(?!)"); // matches nothing

    public ProfanityFilter(ProfanityBlocklistRepository blocklistRepository) {
        this.blocklistRepository = blocklistRepository;
    }

    @PostConstruct
    public void init() {
        reload();
    }

    /**
     * Reload the blocklist from the classpath base file and the database.
     * Thread-safe via volatile field assignment.
     */
    public void reload() {
        Set<String> words = new HashSet<>();

        // Load base word list from classpath resources
        try {
            ClassPathResource resource = new ClassPathResource(BASE_WORDS_PATH);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim().toLowerCase();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        words.add(line);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Could not load base profanity word list from '{}': {}", BASE_WORDS_PATH, e.getMessage());
        }

        // Merge club-specific terms from database
        try {
            blocklistRepository.findAllWords().stream()
                    .filter(w -> w != null && !w.isBlank())
                    .map(String::toLowerCase)
                    .forEach(words::add);
        } catch (Exception e) {
            log.warn("Could not load club-specific profanity terms from database: {}", e.getMessage());
        }

        this.blockedWords = Set.copyOf(words);
        this.blockPattern = buildPattern(words);
        log.info("Profanity filter loaded {} words", words.size());
    }

    /**
     * Check if the given text contains any blocked words.
     *
     * @param text input to check (may be null)
     * @return {@code true} if a blocked word is found (case-insensitive)
     */
    public boolean isBlocked(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return blockPattern.matcher(text).find();
    }

    /**
     * Return the current set of blocked words (unmodifiable copy).
     */
    public Set<String> getBlockedWords() {
        return blockedWords;
    }

    // -------------------------------------------------------------------------

    private Pattern buildPattern(Set<String> words) {
        if (words.isEmpty()) {
            return Pattern.compile("(?!)"); // matches nothing
        }
        String alternation = words.stream()
                .map(Pattern::quote)
                .reduce((a, b) -> a + "|" + b)
                .orElse("");
        String regex = "(?i)\\b(?:" + alternation + ")\\b";
        return Pattern.compile(regex);
    }
}
