package dev.monkeypatch.rctiming.localday.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Provides the {@link RestClient} used by {@code daylifecycle.PreCacheClient} to call the cloud
 * ({@code :app}) module's HTTP API. Plain synchronous {@code RestClient} (Spring 6.1+, not the
 * legacy {@code RestTemplate} or the reactive {@code WebClient}) — this is a single-instance,
 * request-per-call backend with no need for either.
 *
 * <p>Short, fixed connect/read timeouts are deliberate: this venue laptop spends most of its
 * life fully offline (that's the entire premise of this module), so every cloud call must fail
 * fast rather than hang the calling thread waiting on a socket that will never connect.
 */
@Configuration
public class CloudClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);

    @Bean
    public RestClient cloudRestClient(@Value("${localday.cloud.base-url}") String cloudBaseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) CONNECT_TIMEOUT.toMillis());
        requestFactory.setReadTimeout((int) READ_TIMEOUT.toMillis());

        return RestClient.builder()
                .baseUrl(cloudBaseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
