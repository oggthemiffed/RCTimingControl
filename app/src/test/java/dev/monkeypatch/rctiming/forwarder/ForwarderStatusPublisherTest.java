package dev.monkeypatch.rctiming.forwarder;

import dev.monkeypatch.rctiming.timing.LiveTimingHub;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Who owns the decoder state. With the direct listener on (L1), the legacy forwarder must not
 * overwrite it. With the listener off, the forwarder keeps the old behaviour.
 */
class ForwarderStatusPublisherTest {

    @Test
    void directListenerOn_legacyDecoderReportIsIgnored() {
        ForwarderStatusPublisher publisher = new ForwarderStatusPublisher(mock(LiveTimingHub.class), true);

        publisher.onDecoderStatus("CONNECTED");

        assertThat(publisher.getLastKnownStatus().decoderState()).isEqualTo("DISCONNECTED");
    }

    @Test
    void directListenerOn_forwarderDisconnectDoesNotResetDirectDecoderState() {
        ForwarderStatusPublisher publisher = new ForwarderStatusPublisher(mock(LiveTimingHub.class), true);
        publisher.onDirectDecoderStatus("CONNECTED");

        publisher.onForwarderDisconnected();

        assertThat(publisher.getLastKnownStatus().decoderState()).isEqualTo("CONNECTED");
    }

    @Test
    void directListenerOff_legacyDecoderReportIsApplied() {
        ForwarderStatusPublisher publisher = new ForwarderStatusPublisher(mock(LiveTimingHub.class), false);

        publisher.onDecoderStatus("CONNECTED");

        assertThat(publisher.getLastKnownStatus().decoderState()).isEqualTo("CONNECTED");
    }

    @Test
    void directListenerOff_forwarderDisconnectResetsDecoderState() {
        ForwarderStatusPublisher publisher = new ForwarderStatusPublisher(mock(LiveTimingHub.class), false);
        publisher.onDecoderStatus("CONNECTED");

        publisher.onForwarderDisconnected();

        assertThat(publisher.getLastKnownStatus().decoderState()).isEqualTo("DISCONNECTED");
    }
}
