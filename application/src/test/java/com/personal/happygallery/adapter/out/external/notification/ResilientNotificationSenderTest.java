package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.adapter.out.external.resilience.BoundedExecutorFactory;
import com.personal.happygallery.application.notification.port.out.NotificationSendResult;
import com.personal.happygallery.domain.notification.NotificationChannel;
import com.personal.happygallery.domain.notification.NotificationEventType;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.awaitility.Awaitility.await;

class ResilientNotificationSenderTest {

    private static final String IDEMPOTENCY_KEY = "notification-key";

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final List<ThreadPoolTaskExecutor> timeoutExecutors = new ArrayList<>();

    @AfterEach
    void tearDown() {
        timeoutExecutors.forEach(ThreadPoolTaskExecutor::shutdown);
        meterRegistry.close();
    }

    @DisplayName("호출이 시작된 뒤 타임아웃되면 발송 성공 여부를 알 수 없는 결과로 반환한다")
    @Test
    void send_timesOut_returnsDeliveryUnknown() {
        NotificationSender delegate = sender((idempotencyKey, phone, name, eventType) -> {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return NotificationSendResult.SUCCESS;
        });
        NotificationResilienceProperties properties = properties(30, 2, 2, 1, 1);
        NotificationResilienceConfig config = new NotificationResilienceConfig();
        ResilientNotificationSender resilientSender = createSender(
                delegate,
                config.alimtalkNotificationCircuitBreaker(properties, CircuitBreakerRegistry.ofDefaults()),
                config,
                properties);

        NotificationSendResult result = resilientSender.send(
                IDEMPOTENCY_KEY, "01012345678", "홍길동", NotificationEventType.BOOKING_CONFIRMED);

        assertThat(result).isEqualTo(NotificationSendResult.DELIVERY_UNKNOWN);
    }

    @DisplayName("일시 발송 실패가 누적되면 서킷이 열리고 이후 호출을 차단한다")
    @Test
    void send_transientFailuresAccumulate_circuitOpenFastFail() {
        AtomicInteger calls = new AtomicInteger();
        NotificationSender delegate = sender((idempotencyKey, phone, name, eventType) -> {
            calls.incrementAndGet();
            return NotificationSendResult.TRANSIENT_FAILURE;
        });
        NotificationResilienceProperties properties = properties(2, 2, 1, 1);
        NotificationResilienceConfig config = new NotificationResilienceConfig();
        CircuitBreaker circuitBreaker = config.alimtalkNotificationCircuitBreaker(
                properties, CircuitBreakerRegistry.ofDefaults());
        ResilientNotificationSender resilientSender = createSender(delegate, circuitBreaker, config, properties);

        resilientSender.send(IDEMPOTENCY_KEY, "01012345678", "홍길동", NotificationEventType.BOOKING_CONFIRMED);
        resilientSender.send(IDEMPOTENCY_KEY, "01012345678", "홍길동", NotificationEventType.BOOKING_CONFIRMED);
        NotificationSendResult result = resilientSender.send(
                IDEMPOTENCY_KEY, "01012345678", "홍길동", NotificationEventType.BOOKING_CONFIRMED);

        assertSoftly(softly -> {
            softly.assertThat(result).isEqualTo(NotificationSendResult.TRANSIENT_FAILURE);
            softly.assertThat(calls).hasValue(2);
            softly.assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
            softly.assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(2);
        });
    }

    @DisplayName("영구 발송 거절은 다음 채널로 넘기되 서킷 장애율에는 포함하지 않는다")
    @Test
    void send_permanentFailures_doNotOpenCircuit() {
        AtomicInteger calls = new AtomicInteger();
        NotificationSender delegate = sender((idempotencyKey, phone, name, eventType) -> {
            calls.incrementAndGet();
            return NotificationSendResult.PERMANENT_FAILURE;
        });
        NotificationResilienceProperties properties = properties(2, 2, 1, 1);
        NotificationResilienceConfig config = new NotificationResilienceConfig();
        CircuitBreaker circuitBreaker = config.alimtalkNotificationCircuitBreaker(
                properties, CircuitBreakerRegistry.ofDefaults());
        ResilientNotificationSender resilientSender = createSender(delegate, circuitBreaker, config, properties);

        resilientSender.send(IDEMPOTENCY_KEY, "01012345678", "홍길동", NotificationEventType.BOOKING_CONFIRMED);
        resilientSender.send(IDEMPOTENCY_KEY, "01012345678", "홍길동", NotificationEventType.BOOKING_CONFIRMED);
        NotificationSendResult result = resilientSender.send(
                IDEMPOTENCY_KEY, "01012345678", "홍길동", NotificationEventType.BOOKING_CONFIRMED);

        assertSoftly(softly -> {
            softly.assertThat(result).isEqualTo(NotificationSendResult.PERMANENT_FAILURE);
            softly.assertThat(calls).hasValue(3);
            softly.assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
            softly.assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
        });
    }

    @DisplayName("알림 호출 대기열이 가득 차면 즉시 실패하고 거절 횟수를 기록한다")
    @Test
    void send_executorQueueFull_returnsFalseAndRecordsRejection() throws Exception {
        CountDownLatch callStarted = new CountDownLatch(1);
        CountDownLatch releaseCall = new CountDownLatch(1);
        NotificationSender delegate = sender((idempotencyKey, phone, name, eventType) -> {
            callStarted.countDown();
            try {
                releaseCall.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return NotificationSendResult.SUCCESS;
        });
        NotificationResilienceProperties properties = properties(20, 20, 1, 1);
        NotificationResilienceConfig config = new NotificationResilienceConfig();
        ResilientNotificationSender resilientSender = createSender(
                delegate,
                config.alimtalkNotificationCircuitBreaker(properties, CircuitBreakerRegistry.ofDefaults()),
                config,
                properties);

        CompletableFuture<NotificationSendResult> running = CompletableFuture.supplyAsync(() -> resilientSender.send(
                IDEMPOTENCY_KEY, "01011111111", "첫 번째", NotificationEventType.BOOKING_CONFIRMED));
        CompletableFuture<NotificationSendResult> queued = null;
        try {
            assertThat(callStarted.await(1, TimeUnit.SECONDS)).isTrue();
            queued = CompletableFuture.supplyAsync(() -> resilientSender.send(
                    IDEMPOTENCY_KEY, "01022222222", "두 번째", NotificationEventType.BOOKING_CONFIRMED));
            await().atMost(1, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(meterRegistry.get("executor.queued")
                            .tag("name", "alimtalkNotificationTimeoutExecutor")
                            .gauge()
                            .value()).isEqualTo(1));

            NotificationSendResult result = resilientSender.send(
                    IDEMPOTENCY_KEY, "01033333333", "세 번째", NotificationEventType.BOOKING_CONFIRMED);

            assertSoftly(softly -> {
                softly.assertThat(result).isEqualTo(NotificationSendResult.TRANSIENT_FAILURE);
                softly.assertThat(meterRegistry.counter(
                                "happygallery.notification.alimtalk.executor.rejected").count())
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

    @DisplayName("알림톡·일반 SMS·인증 SMS는 서로 다른 제한 실행기와 지표를 사용한다")
    @Test
    void notificationChannels_useIsolatedExecutorsAndMetrics() {
        NotificationResilienceProperties properties = properties(2, 2, 1, 1);
        NotificationResilienceConfig config = new NotificationResilienceConfig();
        BoundedExecutorFactory executorFactory = new BoundedExecutorFactory(
                new ThreadPoolTaskExecutorBuilder(), meterRegistry, task -> task);

        ThreadPoolTaskExecutor alimtalk = register(config.alimtalkNotificationTimeoutExecutor(
                properties, executorFactory));
        ThreadPoolTaskExecutor sms = register(config.smsNotificationTimeoutExecutor(
                properties, executorFactory));
        ThreadPoolTaskExecutor verification = register(config.phoneVerificationTimeoutExecutor(
                properties, executorFactory));

        assertSoftly(softly -> {
            softly.assertThat(List.of(alimtalk, sms, verification)).doesNotHaveDuplicates();
            softly.assertThat(meterRegistry.find("executor.queued")
                    .tag("name", "alimtalkNotificationTimeoutExecutor")
                    .gauge()).isNotNull();
            softly.assertThat(meterRegistry.find("executor.queued")
                    .tag("name", "smsNotificationTimeoutExecutor")
                    .gauge()).isNotNull();
            softly.assertThat(meterRegistry.find("executor.queued")
                    .tag("name", "phoneVerificationTimeoutExecutor")
                    .gauge()).isNotNull();
            softly.assertThat(meterRegistry.find(
                    "happygallery.notification.alimtalk.executor.rejected").counter()).isNotNull();
            softly.assertThat(meterRegistry.find(
                    "happygallery.notification.sms.executor.rejected").counter()).isNotNull();
            softly.assertThat(meterRegistry.find(
                    "happygallery.notification.phone_verification.executor.rejected").counter()).isNotNull();
        });
    }

    private ResilientNotificationSender createSender(NotificationSender delegate,
                                                      CircuitBreaker circuitBreaker,
                                                      NotificationResilienceConfig config,
                                                      NotificationResilienceProperties properties) {
        ThreadPoolTaskExecutor timeoutExecutor = register(config.alimtalkNotificationTimeoutExecutor(
                properties,
                new BoundedExecutorFactory(
                        new ThreadPoolTaskExecutorBuilder(), meterRegistry, task -> task)));
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
        return properties(3_000, slidingWindowSize, minimumNumberOfCalls, poolSize, queueCapacity);
    }

    private static NotificationResilienceProperties properties(long timeoutMillis,
                                                                int slidingWindowSize,
                                                                int minimumNumberOfCalls,
                                                                int poolSize,
                                                                int queueCapacity) {
        var threadPool = new NotificationResilienceProperties.ThreadPool(poolSize, queueCapacity);
        var circuitBreaker = new NotificationResilienceProperties.CircuitBreaker(
                50f, slidingWindowSize, minimumNumberOfCalls, 30, 1);
        return new NotificationResilienceProperties(
                timeoutMillis,
                threadPool,
                threadPool,
                threadPool,
                circuitBreaker);
    }

    private ThreadPoolTaskExecutor register(ThreadPoolTaskExecutor executor) {
        executor.initialize();
        timeoutExecutors.add(executor);
        return executor;
    }

    private static NotificationSender sender(SendBehavior behavior) {
        return new NotificationSender() {
            @Override
            public NotificationChannel channel() {
                return NotificationChannel.KAKAO;
            }

            @Override
            public NotificationSendResult send(String idempotencyKey,
                                               String phone,
                                               String recipientName,
                                               NotificationEventType eventType) {
                return behavior.send(idempotencyKey, phone, recipientName, eventType);
            }
        };
    }

    @FunctionalInterface
    private interface SendBehavior {
        NotificationSendResult send(String idempotencyKey,
                                    String phone,
                                    String recipientName,
                                    NotificationEventType eventType);
    }
}
