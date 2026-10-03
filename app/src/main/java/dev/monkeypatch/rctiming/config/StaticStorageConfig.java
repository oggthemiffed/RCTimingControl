package dev.monkeypatch.rctiming.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Serves {@code FilesystemObjectStorageService}'s uploads back over HTTP at {@code /storage/**}
 * — permitted unauthenticated (GET only) in {@code SecurityConfig}, since club logos and TTS
 * clips are public content. In the trial/production compose stack, nginx's own {@code /storage/}
 * location proxies straight to this app rather than to a separate object-storage container.
 */
@Configuration
public class StaticStorageConfig implements WebMvcConfigurer {

    private final String localPath;

    public StaticStorageConfig(@Value("${storage.local-path}") String localPath) {
        this.localPath = localPath;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(localPath).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/storage/**")
                .addResourceLocations(location);
    }
}
