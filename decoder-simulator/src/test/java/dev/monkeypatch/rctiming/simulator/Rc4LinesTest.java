package dev.monkeypatch.rctiming.simulator;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class Rc4LinesTest {

    @Test
    void writesARecordAsTheDecoderDoes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Rc4Lines.write(out, "#\t20\t1\t72\t0\txDEAD");

        assertThat(out.toString(StandardCharsets.US_ASCII)).isEqualTo("\u0001#\t20\t1\t72\t0\txDEAD\r\n");
    }
}
