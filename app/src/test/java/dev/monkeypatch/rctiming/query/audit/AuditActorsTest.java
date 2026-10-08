package dev.monkeypatch.rctiming.query.audit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditActorsTest {

    @Test
    void readable_dropsTheEmailFromAnOfficialsLabel() {
        assertThat(AuditActors.readable("Race Director <director@club.test>")).isEqualTo("Race Director");
    }

    @Test
    void readable_namesTheSystemJobWithoutItsPrefix() {
        assertThat(AuditActors.readable("system:bump-up")).isEqualTo("System (bump-up)");
    }

    @Test
    void readable_keepsALabelWithNoEmailAndPassesNullThrough() {
        assertThat(AuditActors.readable("official #7")).isEqualTo("official #7");
        assertThat(AuditActors.readable(null)).isNull();
    }
}
