package dev.monkeypatch.rctiming.api.setup.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DecoderConfigUpdateRequest(
        @NotBlank @Size(max = 255) String decoderHost,
        @NotNull @Min(1) @Max(65535) Integer decoderPort,
        @NotNull @Pattern(regexp = "RC4|P3", message = "Protocol must be RC4 or P3") String decoderProtocol
) {}
