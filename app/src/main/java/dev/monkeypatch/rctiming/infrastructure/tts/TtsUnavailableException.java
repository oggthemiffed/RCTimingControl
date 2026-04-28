package dev.monkeypatch.rctiming.infrastructure.tts;

public class TtsUnavailableException extends RuntimeException {
    public TtsUnavailableException(String message) {
        super(message);
    }

    public TtsUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
