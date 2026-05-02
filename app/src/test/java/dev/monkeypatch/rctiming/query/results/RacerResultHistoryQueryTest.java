package dev.monkeypatch.rctiming.query.results;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for RacerResultHistoryQuery (Phase 7, Plan 05).
 * Tests: query returns only results for the requesting user (IDOR safety — RESULT-03);
 *        empty list when user has no results.
 */
@Disabled("Stub — enabled in 07-05-PLAN.md when RacerResultHistoryQuery is implemented")
class RacerResultHistoryQueryTest extends AbstractIntegrationTest {

    @Autowired
    RacerResultHistoryQuery query;

    @Test
    void returnsOnlyResultsForRequestingUser() {
        // TODO (07-05-PLAN.md): user A's results do not appear when queried for user B
        org.junit.jupiter.api.Assertions.fail("Not yet implemented");
    }

    @Test
    void emptyListWhenUserHasNoResults() {
        // TODO (07-05-PLAN.md): new user with no entries returns empty list, not an exception
        org.junit.jupiter.api.Assertions.fail("Not yet implemented");
    }
}
