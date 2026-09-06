package com.personal.happygallery.bootstrap.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AsyncConfigTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("작업은 제출 시점의 MDC를 전달하고 실행 후 빈 문맥을 복원한다")
    void propagatesSubmittedContextAndClearsWorker() {
        Map<String, String> submittedContext = Map.of(
                "requestId", "submitted-request", "operation", "refund");
        MDC.setContextMap(submittedContext);
        Runnable task = new AsyncConfig().asyncContextTaskDecorator().decorate(() -> {
            assertEquals(submittedContext, MDC.getCopyOfContextMap());
            MDC.put("task-only", "value");
        });

        MDC.put("requestId", "later-request");
        MDC.clear();
        task.run();

        assertNull(MDC.getCopyOfContextMap());
    }

    @Test
    @DisplayName("작업이 실패해도 실행 스레드의 기존 MDC를 복원한다")
    void restoresWorkerContextAfterFailure() {
        MDC.put("requestId", "submitted-request");
        Runnable task = new AsyncConfig().asyncContextTaskDecorator().decorate(() -> {
            assertEquals(Map.of("requestId", "submitted-request"), MDC.getCopyOfContextMap());
            MDC.put("task-only", "value");
            throw new IllegalStateException("작업 실패");
        });
        Map<String, String> workerContext = Map.of("worker-only", "previous-value");
        MDC.setContextMap(workerContext);

        assertThrows(IllegalStateException.class, task::run);

        assertEquals(workerContext, MDC.getCopyOfContextMap());
    }

    @Test
    @DisplayName("요청 MDC가 없으면 작업 중 기존 문맥을 지우고 종료 후 복원한다")
    void clearsWorkerContextWhenSubmissionIsEmpty() {
        MDC.clear();
        Runnable task = new AsyncConfig().asyncContextTaskDecorator().decorate(() -> {
            assertNull(MDC.getCopyOfContextMap());
            MDC.put("task-only", "value");
        });
        Map<String, String> workerContext = Map.of("requestId", "previous-request");
        MDC.setContextMap(workerContext);

        task.run();

        assertEquals(workerContext, MDC.getCopyOfContextMap());
    }

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
