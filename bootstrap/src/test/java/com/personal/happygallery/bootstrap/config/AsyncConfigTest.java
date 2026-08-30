package com.personal.happygallery.bootstrap.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AsyncConfigTest {

    @Test
    @DisplayName("커밋 후 실행 신호는 큐가 포화돼도 호출자에게 거절 예외를 전파하지 않는다")
    void suppressesRejectedDurableSignal() throws InterruptedException {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AsyncConfig config = new AsyncConfig();
        ThreadPoolTaskExecutorBuilder builder = new ThreadPoolTaskExecutorBuilder()
                .corePoolSize(1)
                .maxPoolSize(1)
                .queueCapacity(0)
                .awaitTermination(true)
                .awaitTerminationPeriod(Duration.ofSeconds(1));
        ThreadPoolTaskExecutor executor = config.notificationExecutor(
                builder,
                task -> task,
                meterRegistry);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        executor.initialize();

        try {
            executor.execute(() -> {
                started.countDown();
                try {
                    release.await();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            });
            assertTrue(started.await(1, TimeUnit.SECONDS));

            assertDoesNotThrow(() -> executor.execute(() -> {}));
            assertEquals(
                    1.0,
                    meterRegistry.counter(
                            "happygallery.async.executor.rejected",
                            "executor",
                            "notification").count());
        } finally {
            release.countDown();
            executor.shutdown();
            meterRegistry.close();
        }
    }
}
