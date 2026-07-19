package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.domain.notification.NotificationChannel;
import com.personal.happygallery.domain.notification.NotificationEventType;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.awaitility.Awaitility.await;

class ResilientNotificationSenderTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private ExecutorService timeoutExecutor;

    @AfterEach
    void tearDown() {
        if (timeoutExecutor != null) {
            timeoutExecutor.shutdownNow();
        }
        meterRegistry.close();
    }

    @DisplayName("발송 실패 결과가 누적되면 서킷이 열리고 이후 호출을 차단한다")
    @Test
    void send_falseResultsAccumulate_circuitOpenFastFail() {
        AtomicInteger calls = new AtomicInteger();
        NotificationSender delegate = sender((phone, name, eventType) -> {
            calls.incrementAndGet();
            return false;
        });
        NotificationResilienceProperties properties = properties(2, 2, 1, 1);
        NotificationResilienceConfig config = new NotificationResilienceConfig();
        CircuitBreaker circuitBreaker = config.kakaoNotificationCircuitBreaker(properties);
        ResilientNotificationSender resilientSender = createSender(delegate, circuitBreaker, config, properties);

        resilientSender.send("01012345678", "홍길동", NotificationEventType.BOOKING_CONFIRMED);
        resilientSender.send("01012345678", "홍길동", NotificationEventType.BOOKING_CONFIRMED);
        boolean sent = resilientSender.send(
                "01012345678", "홍길동", NotificationEventType.BOOKING_CONFIRMED);

        assertSoftly(softly -> {
            softly.assertThat(sent).isFalse();
            softly.assertThat(calls).hasValue(2);
            softly.assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
            softly.assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(2);
        });
    }

    @DisplayName("알림 호출 대기열이 가득 차면 즉시 실패하고 거절 횟수를 기록한다")
    @Test
    void send_executorQueueFull_returnsFalseAndRecordsRejection() throws Exception {
        CountDownLatch callStarted = new CountDownLatch(1);
        CountDownLatch releaseCall = new CountDownLatch(1);
        NotificationSender delegate = sender((phone, name, eventType) -> {
            callStarted.countDown();
            try {
                releaseCall.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return true;
        });
        NotificationResilienceProperties properties = properties(20, 20, 1, 1);
        NotificationResilienceConfig config = new NotificationResilienceConfig();
        ResilientNotificationSender resilientSender = createSender(
                delegate,
                config.kakaoNotificationCircuitBreaker(properties),
                config,
                properties);

        CompletableFuture<Boolean> running = CompletableFuture.supplyAsync(() -> resilientSender.send(
                "01011111111", "첫 번째", NotificationEventType.BOOKING_CONFIRMED));
        CompletableFuture<Boolean> queued = null;
        try {
            assertThat(callStarted.await(1, TimeUnit.SECONDS)).isTrue();
            queued = CompletableFuture.supplyAsync(() -> resilientSender.send(
                    "01022222222", "두 번째", NotificationEventType.BOOKING_CONFIRMED));
            await().atMost(1, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(meterRegistry.get("executor.queued")
                            .tag("name", "notificationTimeoutExecutor")
                            .gauge()
                            .value()).isEqualTo(1));

            boolean sent = resilientSender.send(
                    "01033333333", "세 번째", NotificationEventType.BOOKING_CONFIRMED);

            assertSoftly(softly -> {
                softly.assertThat(sent).isFalse();
                softly.assertThat(meterRegistry.counter("happygallery.notification.executor.rejected").count())
                        .isEqualTo(1);
            });
        } finally {
            releaseCall.countDown();
            running.join();
            if (queued != null) {
                queued.join();
            }
        }
    }

    private ResilientNotificationSender createSender(NotificationSender delegate,
                                                      CircuitBreaker circuitBreaker,
                                                      NotificationResilienceConfig config,
                                                      NotificationResilienceProperties properties) {
        timeoutExecutor = config.notificationTimeoutExecutor(properties, meterRegistry);
        return new ResilientNotificationSender(
                delegate,
                circuitBreaker,
                config.notificationTimeLimiter(properties),
                timeoutExecutor,
                properties.timeoutMillis());
    }

    private static NotificationResilienceProperties properties(int slidingWindowSize,
                                                                int minimumNumberOfCalls,
                                                                int poolSize,
                                                                int queueCapacity) {
        var threadPool = new NotificationResilienceProperties.ThreadPool(poolSize, queueCapacity);
        var circuitBreaker = new NotificationResilienceProperties.CircuitBreaker(
                50f, slidingWindowSize, minimumNumberOfCalls, 30, 1);
        return new NotificationResilienceProperties(3_000, threadPool, circuitBreaker);
    }

    private static NotificationSender sender(SendBehavior behavior) {
        return new NotificationSender() {
            @Override
            public NotificationChannel channel() {
                return NotificationChannel.KAKAO;
            }

            @Override
            public boolean send(String phone, String recipientName, NotificationEventType eventType) {
                return behavior.send(phone, recipientName, eventType);
            }
        };
    }

    @FunctionalInterface
    private interface SendBehavior {
        boolean send(String phone, String recipientName, NotificationEventType eventType);
    }
}
