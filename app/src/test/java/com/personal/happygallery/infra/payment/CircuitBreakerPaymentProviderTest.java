package com.personal.happygallery.infra.payment;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class CircuitBreakerPaymentProviderTest {

    private CircuitBreakerPaymentProvider provider;

    @AfterEach
    void tearDown() {
        if (provider != null) {
            provider.shutdown();
        }
    }

    @DisplayName("외부 호출 예외가 발생하면 실패 결과를 반환한다")
    @Test
    void refund_delegateThrows_returnsFailure() {
        PaymentProvider delegate = (pgRef, amount) -> {
            throw new RuntimeException("PG error");
        };

        provider = new CircuitBreakerPaymentProvider(delegate, properties(3_000, 50f, 20, 10, 30, 3));

        RefundResult result = provider.refund("pg-ref", 10_000);

        assertSoftly(softly -> {
            softly.assertThat(result.success()).isFalse();
            softly.assertThat(result.failReason()).contains("PG error");
        });
    }

    @DisplayName("외부 호출이 타임아웃을 초과하면 실패 결과를 반환한다")
    @Test
    void refund_delegateTimeout_returnsFailure() {
        PaymentProvider delegate = (pgRef, amount) -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return RefundResult.success("late-ref");
        };

        provider = new CircuitBreakerPaymentProvider(delegate, properties(50, 50f, 20, 10, 30, 3));

        RefundResult result = provider.refund("pg-ref", 10_000);

        assertSoftly(softly -> {
            softly.assertThat(result.success()).isFalse();
            softly.assertThat(result.failReason()).contains("응답 지연");
        });
    }

    @DisplayName("실패가 누적되면 서킷이 열려 빠른 실패를 반환한다")
    @Test
    void refund_failuresAccumulate_circuitOpenFastFail() {
        PaymentProvider delegate = (pgRef, amount) -> {
            throw new RuntimeException("PG down");
        };

        provider = new CircuitBreakerPaymentProvider(delegate, properties(3_000, 50f, 2, 2, 30, 1));

        provider.refund("pg-ref", 10_000);
        provider.refund("pg-ref", 10_000);
        RefundResult result = provider.refund("pg-ref", 10_000);

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
}
