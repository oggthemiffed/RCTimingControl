package dev.monkeypatch.rctiming.domain.entryfeed;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.time.Duration;

/**
 * Downloads an entry feed's file (#42). Never throws: every failure comes back as a {@link Result}, so a feed
 * that can't be reached is shown on the event rather than breaking anything else.
 */
@Component
public class EntryFeedClient {

    private static final int MAX_ERROR_LENGTH = 300;

    private final RestClient restClient;

    public EntryFeedClient(RestClient.Builder restClientBuilder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(20));
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
    }

    public sealed interface Result {
    }

    public record Fetched(String body) implements Result {
    }

    public record AuthFailed(String message) implements Result {
    }

    public record Failed(String message) implements Result {
    }

    /** Fetches {@code url}, sending {@code token} (if any) as a bearer token. */
    public Result fetch(URI url, String token) {
        try {
            String body = restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(h -> {
                        if (token != null) {
                            h.setBearerAuth(token);
                        }
                    })
                    .retrieve()
                    .body(String.class);
            return body == null || body.isBlank() ? new Failed("The feed answered with an empty file") : new Fetched(body);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value()) {
                return new AuthFailed("The feed refused the token (" + status + "). Check the token and save it again.");
            }
            return new Failed(shorten("The feed answered " + status + ": " + e.getResponseBodyAsString()));
        } catch (RuntimeException e) {
            return new Failed(shorten("Couldn't reach the feed: "
                    + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())));
        }
    }

    private static String shorten(String message) {
        return message.length() > MAX_ERROR_LENGTH ? message.substring(0, MAX_ERROR_LENGTH) : message;
    }
}
