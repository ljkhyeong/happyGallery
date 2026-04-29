package com.personal.happygallery.adapter.out.external.payment;

import com.personal.happygallery.application.payment.port.out.PaymentConfirmResult;
import com.personal.happygallery.application.payment.port.out.RefundResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class ResilientPaymentProviderTest {

    private ResilientPaymentProvider provider;
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    @AfterEach
    void tearDown() {
        if (provider != null) {
            provider.shutdown();
        }
        meterRegistry.close();
    }

    @DisplayName("외부 호출 예외가 발생하면 실패 결과를 반환한다")
    @Test
    void refund_delegateThrows_returnsFailure() {
        PaymentProvider delegate = refundOnlyDelegate((pgRef, amount) -> {
            throw new RuntimeException("PG error");
        });

        provider = new ResilientPaymentProvider(delegate, properties(3_000, 50f, 20, 10, 30, 3), meterRegistry);

        RefundResult result = provider.refund("pg-ref", 10_000);

        assertSoftly(softly -> {
            softly.assertThat(result.success()).isFalse();
            softly.assertThat(result.failReason()).contains("PG error");
        });
    }

    @DisplayName("외부 호출이 타임아웃을 초과하면 실패 결과를 반환한다")
    @Test
    void refund_delegateTimeout_returnsFailure() {
        PaymentProvider delegate = refundOnlyDelegate((pgRef, amount) -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return RefundResult.success("late-ref");
        });

        provider = new ResilientPaymentProvider(delegate, properties(50, 50f, 20, 10, 30, 3), meterRegistry);

        RefundResult result = provider.refund("pg-ref", 10_000);

        assertSoftly(softly -> {
            softly.assertThat(result.success()).isFalse();
            softly.assertThat(result.failReason()).contains("응답 지연");
        });
    }

    @DisplayName("실패가 누적되면 서킷이 열려 빠른 실패를 반환한다")
    @Test
    void refund_failuresAccumulate_circuitOpenFastFail() {
        PaymentProvider delegate = refundOnlyDelegate((pgRef, amount) -> {
            throw new RuntimeException("PG down");
        });

        provider = new ResilientPaymentProvider(delegate, properties(3_000, 50f, 2, 2, 30, 1), meterRegistry);

        provider.refund("pg-ref", 10_000);
        provider.refund("pg-ref", 10_000);
        RefundResult result = provider.refund("pg-ref", 10_000);

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

        provider = new ResilientPaymentProvider(delegate, properties(3_000, 50f, 20, 10, 30, 3), meterRegistry);

        PaymentConfirmResult result = provider.confirm("payment-key", "order-id", 10_000);

        assertSoftly(softly -> {
            softly.assertThat(result.success()).isFalse();
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

        provider = new ResilientPaymentProvider(delegate, properties(50, 50f, 20, 10, 30, 3), meterRegistry);

        PaymentConfirmResult result = provider.confirm("payment-key", "order-id", 10_000);

        assertSoftly(softly -> {
            softly.assertThat(result.success()).isFalse();
            softly.assertThat(result.failReason()).contains("응답 지연");
        });
    }

    @DisplayName("결제 확정 실패가 누적되면 서킷이 열려 빠른 실패를 반환한다")
    @Test
    void confirm_failuresAccumulate_circuitOpenFastFail() {
        PaymentProvider delegate = confirmOnlyDelegate((paymentKey, orderId, amount) -> {
            throw new RuntimeException("PG confirm down");
        });

        provider = new ResilientPaymentProvider(delegate, properties(3_000, 50f, 2, 2, 30, 1), meterRegistry);

        provider.confirm("payment-key", "order-id", 10_000);
        provider.confirm("payment-key", "order-id", 10_000);
        PaymentConfirmResult result = provider.confirm("payment-key", "order-id", 10_000);

        assertSoftly(softly -> {
            softly.assertThat(result.success()).isFalse();
            softly.assertThat(result.failReason()).contains("일시 차단");
        });
    }

    private static ExternalPaymentProperties properties(long timeoutMillis,
                                                        float failureRateThreshold,
                                                        int slidingWindowSize,
                                                        int minimumNumberOfCalls,
                                                        long waitDurationOpenSeconds,
                                                        int permittedCallsInHalfOpenState) {
        var circuitBreaker = new ExternalPaymentProperties.CircuitBreaker(
                failureRateThreshold, slidingWindowSize, minimumNumberOfCalls,
                waitDurationOpenSeconds, permittedCallsInHalfOpenState);
        return new ExternalPaymentProperties(timeoutMillis, circuitBreaker);
    }

    private static PaymentProvider refundOnlyDelegate(RefundBehavior refundBehavior) {
        return new PaymentProvider() {
            @Override
            public PaymentConfirmResult confirm(String paymentKey, String orderId, long amount) {
                throw new UnsupportedOperationException();
            }

            @Override
            public RefundResult refund(String pgRef, long amount) {
                return refundBehavior.refund(pgRef, amount);
            }
        };
    }

    private static PaymentProvider confirmOnlyDelegate(ConfirmBehavior confirmBehavior) {
        return new PaymentProvider() {
            @Override
            public PaymentConfirmResult confirm(String paymentKey, String orderId, long amount) {
                return confirmBehavior.confirm(paymentKey, orderId, amount);
            }

            @Override
            public RefundResult refund(String pgRef, long amount) {
                throw new UnsupportedOperationException();
            }
        };
    }

    @FunctionalInterface
    private interface RefundBehavior {
        RefundResult refund(String pgRef, long amount);
    }

    @FunctionalInterface
    private interface ConfirmBehavior {
        PaymentConfirmResult confirm(String paymentKey, String orderId, long amount);
    }
}
