package dev.monkeypatch.rctiming.api.setup;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

@Disabled("Wave 0 stub — enabled in Plan 02 (Setup Backend)")
class SetupControllerIT extends AbstractIntegrationTest {

    @Test
    void getStatus_returnsSetupComplete_false_whenNoClub() {
        fail("Not implemented");
    }

    @Test
    void getStatus_returnsSetupComplete_true_afterClubSaved() {
        fail("Not implemented");
    }

    @Test
    void bootstrap_createsAdminUserAndReturnsToken() {
        fail("Not implemented");
    }

    @Test
    void bootstrap_returns409_whenUsersExist() {
        fail("Not implemented");
    }

    @Test
    void getProgress_reflectsDataState() {
        fail("Not implemented");
    }

    @Test
    void downloadForwarderConfig_returnsEnvAttachment() {
        fail("Not implemented");
    }

    @Test
    void downloadForwarderConfig_includesTokenPlaceholder_notPlaintext() {
        fail("Not implemented");
    }
}
