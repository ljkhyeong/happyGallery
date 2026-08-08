package com.personal.happygallery.adapter.out.external.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.external.payment")
public record ExternalPaymentProperties(
        @NotNull @DurationMin(millis = 1) @DefaultValue("5s") Duration timeout,
        @Valid ThreadPool threadPool,
        @Valid CircuitBreaker circuitBreaker
) {
    public record ThreadPool(
            @Min(1) @DefaultValue("4") int poolSize,
            @Min(1) @DefaultValue("20") int queueCapacity
    ) {}

    public record CircuitBreaker(
            @Min(1) @Max(100) @DefaultValue("50") float failureRateThreshold,
            @Min(1) @DefaultValue("20") int slidingWindowSize,
            @Min(1) @DefaultValue("10") int minimumNumberOfCalls,
            @NotNull @DurationMin(seconds = 1) @DefaultValue("30s") Duration waitDurationOpen,
            @Min(1) @DefaultValue("3") int permittedCallsInHalfOpenState
    ) {
        public CircuitBreaker {
            if (minimumNumberOfCalls > slidingWindowSize) {
                throw new IllegalArgumentException(
                        "결제 circuit breaker의 minimum-number-of-calls는 sliding-window-size 이하여야 합니다.");
            }
        }
    }
}
