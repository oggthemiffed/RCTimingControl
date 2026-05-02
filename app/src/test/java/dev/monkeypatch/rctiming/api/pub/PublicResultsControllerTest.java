package dev.monkeypatch.rctiming.api.pub;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

/**
 * Integration tests for PublicResultsController (Phase 7, Plan 04).
 * Tests: public GET /api/v1/results/{raceId} returns snapshot without auth (RESULT-01);
 *        unknown raceId returns 404 not 403 (RESULT-05 security check T-7-01).
 */
@Disabled("Stub — enabled in 07-04-PLAN.md when PublicResultsController is implemented")
class PublicResultsControllerTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void publicResultsReturnSnapshotWithoutAuth() {
        // TODO (07-04-PLAN.md): GET /api/v1/results/{raceId} returns 200 without Authorization header
        org.junit.jupiter.api.Assertions.fail("Not yet implemented");
    }

    @Test
    void unknownRaceIdReturns404NotForbidden() {
        // TODO (07-04-PLAN.md): GET /api/v1/results/999999 returns 404, not 403
        org.junit.jupiter.api.Assertions.fail("Not yet implemented");
    }
}
