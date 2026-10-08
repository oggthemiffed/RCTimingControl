package dev.monkeypatch.rctiming.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/** The bundled frontend's pages are public; every server path keeps its own rule. */
class SecurityConfigTest {

    @Test
    void pagesAndTheirAssets_areFrontendRequests() {
        assertThat(SecurityConfig.isFrontendRequest(get("/"))).isTrue();
        assertThat(SecurityConfig.isFrontendRequest(get("/race-control/12"))).isTrue();
        assertThat(SecurityConfig.isFrontendRequest(get("/assets/index-abc123.js"))).isTrue();
        assertThat(SecurityConfig.isFrontendRequest(new MockHttpServletRequest("HEAD", "/boards"))).isTrue();
    }

    @Test
    void serverPaths_areNotFrontendRequests() {
        for (String path : new String[] {"/api", "/api/v1/admin/club/profile", "/ws/timing", "/storage/logo.png",
                "/actuator/health", "/error"}) {
            assertThat(SecurityConfig.isFrontendRequest(get(path))).as(path).isFalse();
        }
    }

    @Test
    void anEncodedApiPath_isNotTakenForAPage() {
        MockHttpServletRequest request = get("/%61pi/v1/admin/club/profile");
        assertThat(SecurityConfig.isFrontendRequest(request)).isFalse();
    }

    @Test
    void writes_areNeverFrontendRequests() {
        assertThat(SecurityConfig.isFrontendRequest(new MockHttpServletRequest("POST", "/"))).isFalse();
    }

    private static MockHttpServletRequest get(String path) {
        return new MockHttpServletRequest("GET", path);
    }
}
