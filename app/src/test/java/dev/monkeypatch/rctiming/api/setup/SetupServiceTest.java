package dev.monkeypatch.rctiming.api.setup;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

@Disabled("Wave 0 stub — enabled in Plan 02 (Setup Backend)")
class SetupServiceTest {

    @Test
    void bootstrap_throws_whenAnyUserExists() {
        fail("Not implemented");
    }

    @Test
    void bootstrap_assignsAdminRole_notRacerRole() {
        fail("Not implemented");
    }

    @Test
    void getProgress_returnsAllFalse_onEmptyDb() {
        fail("Not implemented");
    }

    @Test
    void getProgress_returnsClubTrue_whenClubProfileSaved() {
        fail("Not implemented");
    }
}
