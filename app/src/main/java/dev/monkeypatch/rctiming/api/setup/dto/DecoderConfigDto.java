package dev.monkeypatch.rctiming.api.setup.dto;

/** Current decoder connection settings. All fields are null until the decoder is configured. */
public record DecoderConfigDto(String decoderHost, Integer decoderPort, String decoderProtocol) {}
