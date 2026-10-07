package dev.monkeypatch.rctiming.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncConfigTest {

    @Test
    void timingExecutorRunsPassingsOneAtATimeInTheOrderTheyWereSubmitted() throws Exception {
        Executor executor = new AsyncConfig().timingExecutor();
        ((ThreadPoolTaskExecutor) executor).initialize();
        try {
            int count = 200;
            List<Integer> ran = Collections.synchronizedList(new ArrayList<>());
            List<String> threads = Collections.synchronizedList(new ArrayList<>());
            CountDownLatch done = new CountDownLatch(count);
            for (int i = 0; i < count; i++) {
                int n = i;
                executor.execute(() -> {
                    ran.add(n);
                    threads.add(Thread.currentThread().getName());
                    done.countDown();
                });
            }

            assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(ran).isEqualTo(java.util.stream.IntStream.range(0, count).boxed().toList());
            assertThat(threads).hasSize(count).containsOnly(threads.get(0));
            assertThat(threads.get(0)).startsWith("timing-");
        } finally {
            ((ThreadPoolTaskExecutor) executor).shutdown();
        }
    }
}
