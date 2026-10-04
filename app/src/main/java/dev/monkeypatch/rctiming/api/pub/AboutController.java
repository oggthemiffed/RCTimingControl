package dev.monkeypatch.rctiming.api.pub;

import dev.monkeypatch.rctiming.infrastructure.network.LanAddresses;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/about")
public class AboutController {

    private final String version;
    private final Instant buildTime;
    private final LanAddresses lanAddresses;

    public AboutController(BuildProperties buildProperties, LanAddresses lanAddresses) {
        this.version = buildProperties.getVersion();
        this.buildTime = buildProperties.getTime();
        this.lanAddresses = lanAddresses;
    }

    @GetMapping
    public AboutDto get() {
        return new AboutDto(version, buildTime, lanAddresses.urls());
    }

    /** {@code addresses}: the URLs other devices on the venue network can open (#23). */
    public record AboutDto(String version, Instant buildTime, List<String> addresses) {}
}
