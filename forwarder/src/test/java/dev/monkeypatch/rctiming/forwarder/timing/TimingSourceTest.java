package dev.monkeypatch.rctiming.forwarder.timing;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class TimingSourceTest {

    @Test
    void interfaceDeclaresStartAndStop() {
        assertThat(TimingSource.class.getDeclaredMethods())
            .extracting(Method::getName)
            .containsExactlyInAnyOrder("start", "stop");
    }

    @Test
    void rc4ImplementationIsSwappable() {
        // Compile-time substitutability: AmbRc4TimingSource must implement TimingSource
        // This is verified at compile time — if this test compiles and the cast succeeds at runtime, the contract is met.
        Consumer<EpochCorrectedPassing> noopPassing = p -> {};
        Consumer<AmbRc4TimingSource.ConnectionState> noopStatus = s -> {};
        TimingSource source = new AmbRc4TimingSource("localhost", 59999, noopPassing, noopStatus);
        assertThat(source).isInstanceOf(TimingSource.class);
        // Stop immediately so no connection attempt lingers
        source.stop();
    }
}
