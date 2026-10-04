package dev.monkeypatch.rctiming.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Serves the React app built into the jar (#23), so the UI, REST and WebSocket share one port.
 *
 * <p>The packaged build puts the frontend in {@code classpath:/static/}. A path that is not a file
 * there and not a server route gets {@code index.html}, so React Router can handle deep links such
 * as {@code /race-control/12} on refresh. Builds without the frontend (tests, local dev with Vite)
 * have no {@code index.html} and these paths stay 404.
 */
@Configuration
public class SpaConfig implements WebMvcConfigurer {

    static final String LOCATION = "classpath:/static/";

    /** Paths owned by the server; an unknown one is a real 404, not a page of the app. */
    private static final List<String> SERVER_PREFIXES = List.of("api/", "ws/", "storage/", "actuator/", "error");

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Vite fingerprints everything under assets/, so it can be cached for good
        registry.addResourceHandler("/assets/**")
                .addResourceLocations(LOCATION + "assets/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());
        registry.addResourceHandler("/**")
                .addResourceLocations(LOCATION)
                .setCacheControl(CacheControl.noCache())
                .resourceChain(true)
                .addResolver(new IndexFallbackResolver());
    }

    static boolean isServerPath(String path) {
        return SERVER_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private static final class IndexFallbackResolver extends PathResourceResolver {
        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource requested = super.getResource(resourcePath, location);
            if (requested != null) {
                return requested;
            }
            if (isServerPath(resourcePath)) {
                return null;
            }
            Resource index = new ClassPathResource("static/index.html");
            return index.exists() ? index : null;
        }
    }
}
