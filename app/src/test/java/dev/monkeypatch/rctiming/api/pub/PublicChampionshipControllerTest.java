package dev.monkeypatch.rctiming.api.pub;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

/**
 * Integration tests for PublicChampionshipController (Phase 7, Plan 04).
 * Tests: public GET /api/v1/championships/{id} returns standings without auth (CHAMP-05);
 *        unknown id returns 404 not 403.
 */
@Disabled("Stub — enabled in 07-04-PLAN.md when PublicChampionshipController is implemented")
class PublicChampionshipControllerTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void publicStandingsReturnWithoutAuth() {
        // TODO (07-04-PLAN.md): GET /api/v1/championships/{id} returns 200 without Authorization header
        org.junit.jupiter.api.Assertions.fail("Not yet implemented");
    }

    @Test
    void unknownChampionshipIdReturns404NotForbidden() {
        // TODO (07-04-PLAN.md): GET /api/v1/championships/999999 returns 404, not 403
        org.junit.jupiter.api.Assertions.fail("Not yet implemented");
    }
}
