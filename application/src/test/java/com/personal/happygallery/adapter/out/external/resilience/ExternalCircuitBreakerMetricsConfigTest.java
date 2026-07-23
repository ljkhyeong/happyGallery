package com.personal.happygallery.adapter.out.external.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExternalCircuitBreakerMetricsConfigTest {

    @DisplayName("Registry에 등록된 서킷 브레이커의 호출 결과와 상태를 Micrometer에 기록한다")
    @Test
    void circuitBreakerRegistry_recordsCallOutcomeAndStateMetrics() {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        ExternalCircuitBreakerMetricsConfig config = new ExternalCircuitBreakerMetricsConfig();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        try {
            config.externalCircuitBreakerMetrics(circuitBreakerRegistry).bindTo(meterRegistry);
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(
                    "paymentProvider",
                    CircuitBreakerConfig.custom()
                            .slidingWindowSize(2)
                            .minimumNumberOfCalls(2)
                            .failureRateThreshold(50)
                            .waitDurationInOpenState(Duration.ofSeconds(30))
                            .build());

            circuitBreaker.onError(10, TimeUnit.MILLISECONDS, new IllegalStateException());
            circuitBreaker.onSuccess(10, TimeUnit.MILLISECONDS);

            assertThat(meterRegistry.get("resilience4j.circuitbreaker.calls")
                    .tags("name", "paymentProvider", "kind", "failed")
                    .timer()
                    .count()).isEqualTo(1);
            assertThat(meterRegistry.get("resilience4j.circuitbreaker.state")
                    .tags("name", "paymentProvider", "state", "open")
                    .gauge()
                    .value()).isEqualTo(1);

            assertThatThrownBy(() -> circuitBreaker.executeRunnable(() -> {
            })).isInstanceOf(CallNotPermittedException.class);
            assertThat(meterRegistry.get("resilience4j.circuitbreaker.not.permitted.calls")
                    .tag("name", "paymentProvider")
                    .counter()
                    .count()).isEqualTo(1);
        } finally {
            meterRegistry.close();
        }
    }
}
