package dev.monkeypatch.rctiming.api.pub;

import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/about")
public class AboutController {

    private final String version;
    private final Instant buildTime;

    public AboutController(BuildProperties buildProperties) {
        this.version = buildProperties.getVersion();
        this.buildTime = buildProperties.getTime();
    }

    @GetMapping
    public AboutDto get() {
        return new AboutDto(version, buildTime);
    }

    public record AboutDto(String version, Instant buildTime) {}
}
