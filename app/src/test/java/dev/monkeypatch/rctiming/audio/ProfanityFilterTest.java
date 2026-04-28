package dev.monkeypatch.rctiming.audio;

import dev.monkeypatch.rctiming.infrastructure.profanity.ProfanityBlocklistRepository;
import dev.monkeypatch.rctiming.infrastructure.profanity.ProfanityFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfanityFilterTest {

    @Mock
    private ProfanityBlocklistRepository blocklistRepository;

    private ProfanityFilter filter;

    @BeforeEach
    void setUp() {
        when(blocklistRepository.findAllWords()).thenReturn(List.of());
        filter = new ProfanityFilter(blocklistRepository);
        filter.init(); // trigger @PostConstruct
    }

    @Test
    void isBlocked_baseListMatch_returnsTrue() {
        // "shit" is in base-words.txt
        assertTrue(filter.isBlocked("shit"), "Base list word 'shit' should be blocked");
        assertTrue(filter.isBlocked("fuck off"), "'fuck' in phrase should be blocked");
    }

    @Test
    void isBlocked_clubCustomTerm_returnsTrue() {
        // Reload with a custom term from the database
        when(blocklistRepository.findAllWords()).thenReturn(List.of("cheatcode"));
        filter.reload();

        assertTrue(filter.isBlocked("My cheatcode name"), "Club term should be blocked");
        assertFalse(filter.isBlocked("Clean Name"), "Clean text should not be blocked");
    }

    @Test
    void isBlocked_cleanText_returnsFalse() {
        assertFalse(filter.isBlocked("Alan Smith"), "Clean name should not be blocked");
        assertFalse(filter.isBlocked("Fast Racer"), "Clean name should not be blocked");
        assertFalse(filter.isBlocked(""), "Empty string should not be blocked");
        assertFalse(filter.isBlocked(null), "Null should not be blocked");
    }

    @Test
    void isBlocked_caseInsensitive() {
        assertTrue(filter.isBlocked("SHIT"), "Uppercase should match");
        assertTrue(filter.isBlocked("Shit"), "Mixed case should match");
        assertTrue(filter.isBlocked("sHiT"), "Random case should match");
    }

    @Test
    void reload_mergesBaseAndDbTerms() {
        // After reload with extra DB term, both base and custom terms blocked
        when(blocklistRepository.findAllWords()).thenReturn(List.of("BADTERM"));
        filter.reload();

        assertTrue(filter.isBlocked("badterm"), "Custom term (lowercased from DB) should be blocked");
        assertTrue(filter.isBlocked("BADTERM"), "Custom term case-insensitive should be blocked");
        assertTrue(filter.isBlocked("shit"), "Base list words still blocked after reload");
    }

    @Test
    void getBlockedWords_containsBaseWords() {
        assertTrue(filter.getBlockedWords().contains("shit"),
                "getBlockedWords() should include base list entries");
        assertTrue(filter.getBlockedWords().size() >= 10,
                "Should have at least 10 base-list words loaded");
    }
}
