package dev.monkeypatch.rctiming.config;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecureEndpointTest {

    @Test
    void blankSettingsCountAsUnsetAndAreListedAsMissing() {
        URI url = SecureEndpoint.url(URI.create(""), "x.url", "https", "http");
        String token = SecureEndpoint.token("  ");

        assertThat(url).isNull();
        assertThat(token).isNull();
        assertThat(SecureEndpoint.missingSettings(url, "x.url", token, "x.token")).containsExactly("x.url", "x.token");
    }

    @Test
    void theMessageNamesTheSettingAndTheSchemeItNeeds() {
        assertThatThrownBy(() -> SecureEndpoint.url(URI.create("http://racehub.example/api"), "x.url", "https", "http"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("x.url must be a secure https address")
                .hasMessageEndingWith("not http://racehub.example/api");
    }
}
