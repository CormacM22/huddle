package dev.cormac.huddle.infrastructure;

import dev.cormac.huddle.domain.ChatMessage;
import dev.cormac.huddle.domain.MessageRepository;
import dev.cormac.huddle.domain.MessageRepositoryContract;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryMessageRepositoryTest extends MessageRepositoryContract {

    @Override
    protected MessageRepository createRepository() {
        return new InMemoryMessageRepository(1_000);
    }

    @Test
    void evictsOldestMessagesBeyondCapacity() {
        var small = new InMemoryMessageRepository(3);
        for (int i = 1; i <= 5; i++) {
            small.save(message(GENERAL, "m" + i, i));
        }

        assertThat(small.findRecent(GENERAL, 50))
                .extracting(ChatMessage::content)
                .containsExactly("m3", "m4", "m5");
    }

    @Test
    void rejectsNonPositiveCapacity() {
        assertThatThrownBy(() -> new InMemoryMessageRepository(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void concurrentSavesAreNotLost() throws InterruptedException {
        int threads = 8;
        int perThread = 100;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                start.await(); // release all threads at once to maximise contention
                for (int i = 0; i < perThread; i++) {
                    repository.save(message(GENERAL, "msg", i));
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(repository.findRecent(GENERAL, threads * perThread)).hasSize(threads * perThread);
    }
}
