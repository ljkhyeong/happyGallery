package com.personal.happygallery.adapter.out.external.payment;

import com.personal.happygallery.adapter.out.external.resilience.BoundedExecutorFactory;
import com.personal.happygallery.adapter.out.external.resilience.ExternalCircuitBreakerProperties;
import com.personal.happygallery.application.payment.port.out.PaymentConfirmResult;
import com.personal.happygallery.application.payment.port.out.PaymentLookupResult;
import com.personal.happygallery.application.payment.port.out.PaymentPort;
import com.personal.happygallery.application.payment.port.out.RefundLookupResult;
import com.personal.happygallery.application.payment.port.out.RefundResult;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.awaitility.Awaitility.await;

class ResilientPaymentProviderTest {

    private ResilientPaymentProvider provider;
    private ThreadPoolTaskExecutor timeoutExecutor;
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    @AfterEach
    void tearDown() {
        if (timeoutExecutor != null) {
            timeoutExecutor.shutdown();
        }
        meterRegistry.close();
    }

    @DisplayName("환불 외부 호출 결과를 알 수 없으면 상태 확인 필요 결과를 반환한다")
    @Test
    void refund_delegateThrows_returnsFailure() {
        PaymentPort delegate = refundOnlyDelegate((paymentKey, amount) -> {
            throw new RuntimeException("PG error");
        });

        provider = createProvider(delegate, properties(
                Duration.ofSeconds(3), 50f, 20, 10, Duration.ofSeconds(30), 3));

        RefundResult result = provider.refund("payment-key", 10_000, "refund-idempotency-key");

        assertSoftly(softly -> {
            softly.assertThat(result.success()).isFalse();
            softly.assertThat(result.reconciliationRequired()).isTrue();
            softly.assertThat(result.failReason()).isEqualTo("PG 호출 결과를 확인할 수 없습니다.");
        });
    }

    @DisplayName("환불 외부 호출이 타임아웃을 초과하면 상태 확인 필요 결과를 반환한다")
    @Test
    void refund_delegateTimeout_returnsFailure() {
        PaymentPort delegate = refundOnlyDelegate((paymentKey, amount) -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return RefundResult.success("late-ref");
        });

        provider = createProvider(delegate, properties(
                Duration.ofMillis(50), 50f, 20, 10, Duration.ofSeconds(30), 3));

        RefundResult result = provider.refund("payment-key", 10_000, "refund-idempotency-key");

        assertSoftly(softly -> {
            softly.assertThat(result.success()).isFalse();
            softly.assertThat(result.reconciliationRequired()).isTrue();
            softly.assertThat(result.failReason()).contains("응답 지연");
        });
    }

    @DisplayName("PG 호출 대기열이 가득 차면 즉시 재시도 가능 결과를 반환하고 거절 횟수를 기록한다")
    @Test
    void refund_executorQueueFull_returnsRetryableFailureAndRecordsRejection() throws Exception {
        CountDownLatch callStarted = new CountDownLatch(1);
        CountDownLatch releaseCall = new CountDownLatch(1);
        PaymentPort delegate = refundOnlyDelegate((paymentKey, amount) -> {
            callStarted.countDown();
            try {
                releaseCall.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return RefundResult.success("refund-transaction-key");
        });
        provider = createProvider(
                delegate,
                properties(
                        Duration.ofSeconds(3),
                        50f,
                        20,
                        10,
                        Duration.ofSeconds(30),
                        3,
                        1,
                        1));

        CompletableFuture<RefundResult> running = CompletableFuture.supplyAsync(
                () -> provider.refund("payment-key-1", 10_000, "idempotency-key-1"));
        CompletableFuture<RefundResult> queued = null;
        try {
            assertThat(callStarted.await(1, TimeUnit.SECONDS)).isTrue();
            queued = CompletableFuture.supplyAsync(
                    () -> provider.refund("payment-key-2", 10_000, "idempotency-key-2"));
            await().atMost(1, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(timeoutExecutor.getThreadPoolExecutor().getQueue()).hasSize(1));

            RefundResult rejected = provider.refund("payment-key-3", 10_000, "idempotency-key-3");

            assertSoftly(softly -> {
                softly.assertThat(rejected.retryable()).isTrue();
                softly.assertThat(rejected.failReason()).contains("대기열");
                softly.assertThat(meterRegistry.counter("happygallery.payment.executor.rejected").count())
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

    @DisplayName("실패가 누적되면 서킷이 열려 빠른 실패를 반환한다")
    @Test
    void refund_failuresAccumulate_circuitOpenFastFail() {
        PaymentPort delegate = refundOnlyDelegate(
                (paymentKey, amount) -> RefundResult.retryableFailure("PG down"));

        provider = createProvider(delegate, properties(
                Duration.ofSeconds(3), 50f, 2, 2, Duration.ofSeconds(30), 1));

        provider.refund("payment-key", 10_000, "refund-idempotency-key");
        provider.refund("payment-key", 10_000, "refund-idempotency-key");
        RefundResult result = provider.refund("payment-key", 10_000, "refund-idempotency-key");

        assertSoftly(softly -> {
            softly.assertThat(result.success()).isFalse();
            softly.assertThat(result.failReason()).contains("일시 차단");
        });
    }

    @DisplayName("결제 확정 외부 호출 예외가 발생하면 실패 결과를 반환한다")
    @Test
    void confirm_delegateThrows_returnsFailure() {
        PaymentPort delegate = confirmOnlyDelegate((paymentKey, orderId, amount) -> {
            throw new RuntimeException("PG confirm error");
        });

        provider = createProvider(delegate, properties(
                Duration.ofSeconds(3), 50f, 20, 10, Duration.ofSeconds(30), 3));

        PaymentConfirmResult result = provider.confirm(
                "payment-key", "order-id", 10_000, "confirm-idempotency-key");

        assertSoftly(softly -> {
            softly.assertThat(result.success()).isFalse();
            softly.assertThat(result.retryable()).isTrue();
            softly.assertThat(result.failReason()).isEqualTo("PG 호출 중 오류가 발생했습니다.");
        });
    }

    @DisplayName("결제 확정 외부 호출이 타임아웃을 초과하면 실패 결과를 반환한다")
    @Test
    void confirm_delegateTimeout_returnsFailure() {
        PaymentPort delegate = confirmOnlyDelegate((paymentKey, orderId, amount) -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return PaymentConfirmResult.success("late-ref", "CARD", "2026-04-23T10:00:00+09:00");
        });

        provider = createProvider(delegate, properties(
                Duration.ofMillis(50), 50f, 20, 10, Duration.ofSeconds(30), 3));

        PaymentConfirmResult result = provider.confirm(
                "payment-key", "order-id", 10_000, "confirm-idempotency-key");

        assertSoftly(softly -> {
            softly.assertThat(result.success()).isFalse();
            softly.assertThat(result.retryable()).isTrue();
            softly.assertThat(result.failReason()).contains("응답 지연");
        });
    }

    @DisplayName("결제 확정 실패가 누적되면 서킷이 열려 빠른 실패를 반환한다")
    @Test
    void confirm_failuresAccumulate_circuitOpenFastFail() {
        PaymentPort delegate = confirmOnlyDelegate(
                (paymentKey, orderId, amount) -> PaymentConfirmResult.retryableFailure("PG confirm down"));

        provider = createProvider(delegate, properties(
                Duration.ofSeconds(3), 50f, 2, 2, Duration.ofSeconds(30), 1));

        provider.confirm("payment-key", "order-id", 10_000, "confirm-idempotency-key");
        provider.confirm("payment-key", "order-id", 10_000, "confirm-idempotency-key");
        PaymentConfirmResult result = provider.confirm(
                "payment-key", "order-id", 10_000, "confirm-idempotency-key");

        assertSoftly(softly -> {
            softly.assertThat(result.success()).isFalse();
            softly.assertThat(result.retryable()).isTrue();
            softly.assertThat(result.failReason()).contains("일시 차단");
        });
    }

    @DisplayName("자동 판정이 어려운 정상 PG 상태 조회는 결제 서킷을 열지 않는다")
    @Test
    void lookup_reviewRequired_doesNotOpenSharedCircuit() {
        PaymentPort delegate = lookupOnlyDelegate(orderId ->
                PaymentLookupResult.reviewRequired(orderId, "PG 결제가 처리 중입니다."));
        provider = createProvider(delegate, properties(
                Duration.ofSeconds(3), 50f, 2, 2, Duration.ofSeconds(30), 1));

        provider.lookupByOrderId("order-id-1");
        provider.lookupByOrderId("order-id-2");
        PaymentLookupResult result = provider.lookupByOrderId("order-id-3");

        assertThat(result.status()).isEqualTo(PaymentLookupResult.Status.REVIEW_REQUIRED);
    }

    @DisplayName("환불 조회 불가가 누적되면 서킷이 열려 조회를 빠르게 차단한다")
    @Test
    void lookupRefund_unavailableAccumulates_circuitOpenFastFail() {
        PaymentPort delegate = refundLookupOnlyDelegate((paymentKey, amount, idempotencyKey) ->
                RefundLookupResult.unavailable(paymentKey, "PG 환불 조회 실패"));
        provider = createProvider(delegate, properties(
                Duration.ofSeconds(3), 50f, 2, 2, Duration.ofSeconds(30), 1));

        provider.lookupRefund("payment-key-1", 10_000L, "idempotency-key-1");
        provider.lookupRefund("payment-key-2", 10_000L, "idempotency-key-2");
        RefundLookupResult result = provider.lookupRefund(
                "payment-key-3", 10_000L, "idempotency-key-3");

        assertSoftly(softly -> {
            softly.assertThat(result.status()).isEqualTo(RefundLookupResult.Status.UNAVAILABLE);
            softly.assertThat(result.reason()).contains("일시 차단");
        });
    }

    @DisplayName("결제 TimeLimiter는 Toss transport 제한 합보다 길어야 한다")
    @Test
    void paymentTimeLimiter_requiresLongerTimeoutThanTossTransportBudget() {
        PaymentResilienceConfig config = new PaymentResilienceConfig();
        ExternalPaymentProperties valid = properties(
                Duration.ofSeconds(5), 50f, 20, 10, Duration.ofSeconds(30), 3);
        TossPaymentsProperties transport = tossProperties(
                Duration.ofSeconds(3), Duration.ofSeconds(1), Duration.ofMillis(500));

        assertThat(config.paymentTimeLimiter(valid, transport)
                .getTimeLimiterConfig()
                .getTimeoutDuration()).isEqualTo(Duration.ofMillis(5_000));
        assertThatThrownBy(() -> config.paymentTimeLimiter(
                properties(
                        Duration.ofMillis(4_500),
                        50f,
                        20,
                        10,
                        Duration.ofSeconds(30),
                        3),
                transport))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("acquire + connect + response");
    }

    private ResilientPaymentProvider createProvider(PaymentPort delegate,
                                                    ExternalPaymentProperties properties) {
        PaymentResilienceConfig config = new PaymentResilienceConfig();
        timeoutExecutor = config.paymentTimeoutExecutor(
                properties,
                new BoundedExecutorFactory(
                        new ThreadPoolTaskExecutorBuilder(), meterRegistry, task -> task));
        timeoutExecutor.initialize();
        return config.resilientPaymentProvider(
                delegate,
                config.paymentCircuitBreaker(properties, CircuitBreakerRegistry.ofDefaults()),
                config.paymentTimeLimiter(properties, tossPropertiesFor(properties.timeout())),
                timeoutExecutor,
                properties);
    }

    private static ExternalPaymentProperties properties(Duration timeout,
                                                        float failureRateThreshold,
                                                        int slidingWindowSize,
                                                        int minimumNumberOfCalls,
                                                        Duration waitDurationOpen,
                                                        int permittedCallsInHalfOpenState) {
        return properties(
                timeout,
                failureRateThreshold,
                slidingWindowSize,
                minimumNumberOfCalls,
                waitDurationOpen,
                permittedCallsInHalfOpenState,
                4,
                20);
    }

    private static ExternalPaymentProperties properties(Duration timeout,
                                                        float failureRateThreshold,
                                                        int slidingWindowSize,
                                                        int minimumNumberOfCalls,
                                                        Duration waitDurationOpen,
                                                        int permittedCallsInHalfOpenState,
                                                        int poolSize,
                                                        int queueCapacity) {
        var circuitBreaker = new ExternalCircuitBreakerProperties(
                failureRateThreshold, slidingWindowSize, minimumNumberOfCalls,
                waitDurationOpen, permittedCallsInHalfOpenState);
        var threadPool = new ExternalPaymentProperties.ThreadPool(poolSize, queueCapacity);
        return new ExternalPaymentProperties(timeout, threadPool, circuitBreaker);
    }

    private static TossPaymentsProperties tossPropertiesFor(Duration outerTimeout) {
        Duration phaseTimeout = outerTimeout.dividedBy(4);
        if (!phaseTimeout.isPositive()) {
            phaseTimeout = Duration.ofMillis(1);
        }
        return tossProperties(phaseTimeout, phaseTimeout, phaseTimeout);
    }

    private static TossPaymentsProperties tossProperties(Duration responseTimeout,
                                                          Duration connectTimeout,
                                                          Duration acquireTimeout) {
        return new TossPaymentsProperties(
                "",
                "https://api.tosspayments.com",
                responseTimeout,
                connectTimeout,
                acquireTimeout,
                10,
                Duration.ofSeconds(30));
    }

    private static PaymentPort refundOnlyDelegate(RefundBehavior refundBehavior) {
        return new PaymentPort() {
            @Override
            public PaymentConfirmResult confirm(String paymentKey, String orderId, long amount,
                                                String idempotencyKey) {
                throw new UnsupportedOperationException();
            }

            @Override
            public RefundResult refund(String paymentKey, long amount, String idempotencyKey) {
                return refundBehavior.refund(paymentKey, amount);
            }

            @Override
            public PaymentLookupResult lookupByOrderId(String orderId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public RefundLookupResult lookupRefund(
                    String paymentKey, long amount, String idempotencyKey) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static PaymentPort confirmOnlyDelegate(ConfirmBehavior confirmBehavior) {
        return new PaymentPort() {
            @Override
            public PaymentConfirmResult confirm(String paymentKey, String orderId, long amount,
                                                String idempotencyKey) {
                return confirmBehavior.confirm(paymentKey, orderId, amount);
            }

            @Override
            public RefundResult refund(String paymentKey, long amount, String idempotencyKey) {
                throw new UnsupportedOperationException();
            }

            @Override
            public PaymentLookupResult lookupByOrderId(String orderId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public RefundLookupResult lookupRefund(
                    String paymentKey, long amount, String idempotencyKey) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static PaymentPort lookupOnlyDelegate(LookupBehavior lookupBehavior) {
        return new PaymentPort() {
            @Override
            public PaymentConfirmResult confirm(String paymentKey, String orderId, long amount,
                                                String idempotencyKey) {
                throw new UnsupportedOperationException();
            }

            @Override
            public RefundResult refund(String paymentKey, long amount, String idempotencyKey) {
                throw new UnsupportedOperationException();
            }

            @Override
            public PaymentLookupResult lookupByOrderId(String orderId) {
                return lookupBehavior.lookup(orderId);
            }

            @Override
            public RefundLookupResult lookupRefund(
                    String paymentKey, long amount, String idempotencyKey) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static PaymentPort refundLookupOnlyDelegate(RefundLookupBehavior lookupBehavior) {
        return new PaymentPort() {
            @Override
            public PaymentConfirmResult confirm(String paymentKey, String orderId, long amount,
                                                String idempotencyKey) {
                throw new UnsupportedOperationException();
            }

            @Override
            public RefundResult refund(String paymentKey, long amount, String idempotencyKey) {
                throw new UnsupportedOperationException();
            }

            @Override
            public PaymentLookupResult lookupByOrderId(String orderId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public RefundLookupResult lookupRefund(
                    String paymentKey, long amount, String idempotencyKey) {
                return lookupBehavior.lookup(paymentKey, amount, idempotencyKey);
            }
        };
    }

    @FunctionalInterface
    private interface RefundBehavior {
        RefundResult refund(String paymentKey, long amount);
    }

    @FunctionalInterface
    private interface ConfirmBehavior {
        PaymentConfirmResult confirm(String paymentKey, String orderId, long amount);
    }

    @FunctionalInterface
    private interface LookupBehavior {
        PaymentLookupResult lookup(String orderId);
    }

    @FunctionalInterface
    private interface RefundLookupBehavior {
        RefundLookupResult lookup(String paymentKey, long amount, String idempotencyKey);
    }
}
