package dev.monkeypatch.rctiming.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NamesTest {

    @Test
    void namesMatchIgnoringCapitalsAndSpacing() {
        assertThat(Names.matchKey("  Alex\t Rowe ")).isEqualTo(Names.matchKey("alexrowe"));
        assertThat(Names.matchKey("Stock  Buggy")).isEqualTo(Names.matchKey("stock buggy"));
        assertThat(Names.matchKey("Alex Rowe")).isEqualTo("alexrowe");
    }

    @Test
    void accentedCapitalsFoldButAccentsStay() {
        assertThat(Names.matchKey("RENÉ MÜLLER")).isEqualTo("renémüller");
        assertThat(Names.matchKey("Rene Muller")).isNotEqualTo(Names.matchKey("René Müller"));
    }
}
