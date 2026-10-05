package dev.monkeypatch.rctiming.infrastructure.network;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The addresses other devices are told to open (#23) must be URLs a browser accepts. */
class LanAddressesTest {

    @Test
    void buildsUrlsForIpv4Ipv6AndHostnames() {
        assertThat(LanAddresses.url("192.168.1.20", 8080)).isEqualTo("http://192.168.1.20:8080/");
        assertThat(LanAddresses.url("2001:db8::50", 8080)).isEqualTo("http://[2001:db8::50]:8080/");
        assertThat(LanAddresses.url("[2001:db8::50]", 8080)).isEqualTo("http://[2001:db8::50]:8080/");
        assertThat(LanAddresses.url("timing-laptop", 80)).isEqualTo("http://timing-laptop/");
    }
}
