package dev.monkeypatch.rctiming.persistence;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import dev.monkeypatch.rctiming.domain.competitor.Competitor;
import dev.monkeypatch.rctiming.domain.competitor.CompetitorRepository;
import dev.monkeypatch.rctiming.query.competitor.CompetitorQueryService;
import dev.monkeypatch.rctiming.query.competitor.CompetitorSummaryDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Read-side queries run in a {@link ReadTransaction} on the read pool (#143), so a spectator board or
 * an official's screen never queues behind the single write connection while a write is in progress.
 */
class ReadTransactionIT extends AbstractIntegrationTest {

    @Autowired CompetitorQueryService competitorQueryService;
    @Autowired CompetitorRepository competitorRepository;
    @Autowired TransactionTemplate transactionTemplate;

    @Test
    void aReadQuery_doesNotWaitForAWriteInProgress_andSeesOnlyCommittedData() throws Exception {
        String name = "Uncommitted " + UUID.randomUUID();
        CountDownLatch written = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        // Its own threads: the writer blocks one for the whole test, which could starve the common pool
        ExecutorService threads = Executors.newFixedThreadPool(2);
        CompletableFuture<Void> writer = CompletableFuture.runAsync(() ->
                transactionTemplate.executeWithoutResult(status -> {
                    competitorRepository.save(competitor(name));
                    written.countDown();
                    try {
                        release.await(30, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }), threads);
        try {
            assertThat(written.await(10, TimeUnit.SECONDS)).isTrue();

            // The write connection is held by the open transaction; on it this would block until released
            CompletableFuture<Boolean> sawIt = CompletableFuture.supplyAsync(() -> listsCompetitor(name), threads);
            assertThat(sawIt.get(5, TimeUnit.SECONDS)).isFalse();
        } finally {
            release.countDown();
            writer.get(10, TimeUnit.SECONDS);
            threads.shutdownNow();
        }
        assertThat(listsCompetitor(name)).isTrue();
    }

    @Test
    void aReadQueryInsideAWriteTransaction_seesThatTransactionsWrites() {
        String name = "Same transaction " + UUID.randomUUID();
        Boolean sawIt = transactionTemplate.execute(status -> {
            competitorRepository.save(competitor(name));
            boolean seen = listsCompetitor(name);
            status.setRollbackOnly();
            return seen;
        });
        assertThat(sawIt).isTrue();
        assertThat(listsCompetitor(name)).isFalse();
    }

    private boolean listsCompetitor(String name) {
        return competitorQueryService.listAll().stream()
                .map(CompetitorSummaryDto::displayName)
                .anyMatch(name::equals);
    }

    private static Competitor competitor(String name) {
        Competitor competitor = new Competitor();
        competitor.setDisplayName(name);
        competitor.setCreatedAt(Instant.now());
        competitor.setUpdatedAt(Instant.now());
        return competitor;
    }
}
