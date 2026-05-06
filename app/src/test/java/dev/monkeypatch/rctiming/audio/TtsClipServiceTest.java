package dev.monkeypatch.rctiming.audio;

import dev.monkeypatch.rctiming.infrastructure.storage.ObjectStorageService;
import dev.monkeypatch.rctiming.infrastructure.tts.PiperTtsClient;
import dev.monkeypatch.rctiming.infrastructure.tts.TtsClipService;
import dev.monkeypatch.rctiming.infrastructure.tts.TtsProperties;
import dev.monkeypatch.rctiming.infrastructure.tts.TtsUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TtsClipServiceTest {

    @Mock
    private PiperTtsClient piperClient;

    @Mock
    private ObjectStorageService storageService;

    private TtsClipService service;

    private static final byte[] FAKE_WAV = new byte[]{0x52, 0x49, 0x46, 0x46}; // "RIFF"
    private static final TtsProperties PROPS =
            new TtsProperties("piper:10200", "en_GB-alan-medium", true, null);

    @BeforeEach
    void setUp() {
        service = new TtsClipService(piperClient, storageService, PROPS);
    }

    @Test
    void generateNameClip_storesInMinIO_returnsUrl() {
        when(piperClient.synthesize("Alan Smith", "en_GB-alan-medium")).thenReturn(FAKE_WAV);
        when(storageService.upload(
                eq("audio/racer/42/name-en_GB-alan-medium.wav"),
                eq(FAKE_WAV),
                eq("audio/wav")))
                .thenReturn("http://minio/rctiming/audio/racer/42/name-en_GB-alan-medium.wav");

        String url = service.generateNameClip(42L, "Alan Smith", null); // null → default voice

        assertNotNull(url);
        assertTrue(url.contains("audio/racer/42"));
        verify(storageService).upload(
                eq("audio/racer/42/name-en_GB-alan-medium.wav"),
                eq(FAKE_WAV),
                eq("audio/wav"));
    }

    @Test
    void generateNameClip_voiceChanged_regeneratesClip() {
        when(piperClient.synthesize("Bob Jones", "en_GB-cori-high")).thenReturn(FAKE_WAV);
        when(storageService.upload(
                eq("audio/racer/7/name-en_GB-cori-high.wav"),
                any(), any()))
                .thenReturn("http://minio/rctiming/audio/racer/7/name-en_GB-cori-high.wav");

        String url = service.generateNameClip(7L, "Bob Jones", "en_GB-cori-high");

        assertNotNull(url);
        verify(piperClient).synthesize("Bob Jones", "en_GB-cori-high");
        verify(storageService).upload(eq("audio/racer/7/name-en_GB-cori-high.wav"), any(), any());
    }

    @Test
    void generateNameClip_piperUnavailable_returnsNull() {
        when(piperClient.synthesize(any(), any()))
                .thenThrow(new TtsUnavailableException("Piper is down"));

        String url = service.generateNameClip(1L, "Test Racer", null);

        assertNull(url, "Should return null when Piper is unavailable");
        verify(storageService, never()).upload(any(), any(), any());
    }

    @Test
    void generateCountdownClip_storesWithCorrectKey() {
        when(piperClient.synthesize(eq("Race Finals, 5 minutes"), anyString())).thenReturn(FAKE_WAV);
        when(storageService.upload(
                eq("audio/race/10/countdown-300-en_GB-alan-medium.wav"),
                any(), any()))
                .thenReturn("http://minio/rctiming/audio/race/10/countdown-300-en_GB-alan-medium.wav");

        String url = service.generateCountdownClip(10L, 300, "Race Finals, 5 minutes", null);

        assertNotNull(url);
        verify(storageService).upload(
                eq("audio/race/10/countdown-300-en_GB-alan-medium.wav"),
                eq(FAKE_WAV), eq("audio/wav"));
    }

    @Test
    void generateCarNumberClip_storesWithCorrectKey() {
        when(piperClient.synthesize(eq("Car 42"), anyString())).thenReturn(FAKE_WAV);
        when(storageService.upload(
                eq("audio/race/5/car-42-en_GB-alan-medium.wav"),
                any(), any()))
                .thenReturn("http://minio/");

        service.generateCarNumberClip(5L, 42, "Car 42", null);

        verify(storageService).upload(
                eq("audio/race/5/car-42-en_GB-alan-medium.wav"),
                eq(FAKE_WAV), eq("audio/wav"));
    }

    @Test
    void generatePreview_returnsBytesWithoutStoring() {
        when(piperClient.synthesize("Alice", "en_GB-alan-medium")).thenReturn(FAKE_WAV);

        byte[] result = service.generatePreview("Alice", null);

        assertArrayEquals(FAKE_WAV, result);
        verify(storageService, never()).upload(any(), any(), any());
    }
}
