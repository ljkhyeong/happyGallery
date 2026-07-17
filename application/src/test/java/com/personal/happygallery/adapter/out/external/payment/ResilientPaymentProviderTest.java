package com.personal.happygallery.adapter.out.external.payment;

import com.personal.happygallery.application.payment.port.out.PaymentConfirmResult;
import com.personal.happygallery.application.payment.port.out.RefundResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.awaitility.Awaitility.await;

class ResilientPaymentProviderTest {

    private ResilientPaymentProvider provider;
    private PaymentTimeoutExecutor timeoutExecutor;
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    @AfterEach
    void tearDown() {
        if (timeoutExecutor != null) {
            timeoutExecutor.close();
        }
        meterRegistry.close();
    }

    @DisplayName("환불 외부 호출 결과를 알 수 없으면 상태 확인 필요 결과를 반환한다")
    @Test
    void refund_delegateThrows_returnsFailure() {
        PaymentProvider delegate = refundOnlyDelegate((paymentKey, amount) -> {
            throw new RuntimeException("PG error");
        });

        provider = createProvider(delegate, properties(3_000, 50f, 20, 10, 30, 3));

        RefundResult result = provider.refund("payment-key", 10_000, "refund-idempotency-key");

        assertSoftly(softly -> {
            softly.assertThat(result.success()).isFalse();
            softly.assertThat(result.reconciliationRequired()).isTrue();
            softly.assertThat(result.failReason()).contains("PG error");
        });
    }

    @DisplayName("환불 외부 호출이 타임아웃을 초과하면 상태 확인 필요 결과를 반환한다")
    @Test
    void refund_delegateTimeout_returnsFailure() {
        PaymentProvider delegate = refundOnlyDelegate((paymentKey, amount) -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return RefundResult.success("late-ref");
        });

        provider = createProvider(delegate, properties(50, 50f, 20, 10, 30, 3));

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
        PaymentProvider delegate = refundOnlyDelegate((paymentKey, amount) -> {
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
                properties(3_000, 50f, 20, 10, 30, 3, 1, 1));

        CompletableFuture<RefundResult> running = CompletableFuture.supplyAsync(
                () -> provider.refund("payment-key-1", 10_000, "idempotency-key-1"));
        CompletableFuture<RefundResult> queued = null;
        try {
            assertThat(callStarted.await(1, TimeUnit.SECONDS)).isTrue();
            queued = CompletableFuture.supplyAsync(
                    () -> provider.refund("payment-key-2", 10_000, "idempotency-key-2"));
            await().atMost(1, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(meterRegistry.get("executor.queued")
                            .tag("name", "paymentTimeoutExecutor")
                            .gauge()
                            .value()).isEqualTo(1));

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
        PaymentProvider delegate = refundOnlyDelegate(
                (paymentKey, amount) -> RefundResult.retryableFailure("PG down"));

        provider = createProvider(delegate, properties(3_000, 50f, 2, 2, 30, 1));

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
        PaymentProvider delegate = confirmOnlyDelegate((paymentKey, orderId, amount) -> {
            throw new RuntimeException("PG confirm error");
        });

        provider = createProvider(delegate, properties(3_000, 50f, 20, 10, 30, 3));

        PaymentConfirmResult result = provider.confirm(
                "payment-key", "order-id", 10_000, "confirm-idempotency-key");

        assertSoftly(softly -> {
            softly.assertThat(result.success()).isFalse();
            softly.assertThat(result.retryable()).isTrue();
            softly.assertThat(result.failReason()).contains("PG confirm error");
        });
    }

    @DisplayName("결제 확정 외부 호출이 타임아웃을 초과하면 실패 결과를 반환한다")
    @Test
    void confirm_delegateTimeout_returnsFailure() {
        PaymentProvider delegate = confirmOnlyDelegate((paymentKey, orderId, amount) -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return PaymentConfirmResult.success("late-ref", "CARD", "2026-04-23T10:00:00+09:00");
        });

        provider = createProvider(delegate, properties(50, 50f, 20, 10, 30, 3));

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
        PaymentProvider delegate = confirmOnlyDelegate(
                (paymentKey, orderId, amount) -> PaymentConfirmResult.retryableFailure("PG confirm down"));

        provider = createProvider(delegate, properties(3_000, 50f, 2, 2, 30, 1));

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

    private ResilientPaymentProvider createProvider(PaymentProvider delegate,
                                                    ExternalPaymentProperties properties) {
        PaymentResilienceConfig config = new PaymentResilienceConfig();
        timeoutExecutor = config.paymentTimeoutExecutor(properties, meterRegistry);
        return config.resilientPaymentProvider(
                delegate,
                config.paymentCircuitBreaker(properties),
                config.paymentTimeLimiter(properties),
                timeoutExecutor,
                properties);
    }

    private static ExternalPaymentProperties properties(long timeoutMillis,
                                                        float failureRateThreshold,
                                                        int slidingWindowSize,
                                                        int minimumNumberOfCalls,
                                                        long waitDurationOpenSeconds,
                                                        int permittedCallsInHalfOpenState) {
        return properties(
                timeoutMillis,
                failureRateThreshold,
                slidingWindowSize,
                minimumNumberOfCalls,
                waitDurationOpenSeconds,
                permittedCallsInHalfOpenState,
                4,
                20);
    }

    private static ExternalPaymentProperties properties(long timeoutMillis,
                                                        float failureRateThreshold,
                                                        int slidingWindowSize,
                                                        int minimumNumberOfCalls,
                                                        long waitDurationOpenSeconds,
                                                        int permittedCallsInHalfOpenState,
                                                        int poolSize,
                                                        int queueCapacity) {
        var circuitBreaker = new ExternalPaymentProperties.CircuitBreaker(
                failureRateThreshold, slidingWindowSize, minimumNumberOfCalls,
                waitDurationOpenSeconds, permittedCallsInHalfOpenState);
        var threadPool = new ExternalPaymentProperties.ThreadPool(poolSize, queueCapacity);
        return new ExternalPaymentProperties(timeoutMillis, threadPool, circuitBreaker);
    }

    private static PaymentProvider refundOnlyDelegate(RefundBehavior refundBehavior) {
        return new PaymentProvider() {
            @Override
            public PaymentConfirmResult confirm(String paymentKey, String orderId, long amount,
                                                String idempotencyKey) {
                throw new UnsupportedOperationException();
            }

            @Override
            public RefundResult refund(String paymentKey, long amount, String idempotencyKey) {
                return refundBehavior.refund(paymentKey, amount);
            }
        };
    }

    private static PaymentProvider confirmOnlyDelegate(ConfirmBehavior confirmBehavior) {
        return new PaymentProvider() {
            @Override
            public PaymentConfirmResult confirm(String paymentKey, String orderId, long amount,
                                                String idempotencyKey) {
                return confirmBehavior.confirm(paymentKey, orderId, amount);
            }

            @Override
            public RefundResult refund(String paymentKey, long amount, String idempotencyKey) {
                throw new UnsupportedOperationException();
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
}
