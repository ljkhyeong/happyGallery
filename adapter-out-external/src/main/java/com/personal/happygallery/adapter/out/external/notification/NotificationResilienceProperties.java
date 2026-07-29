package com.personal.happygallery.adapter.out.external.notification;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.external.notification")
public record NotificationResilienceProperties(
        @Min(1) @DefaultValue("5000") long timeoutMillis,
        @Valid ThreadPool alimtalkThreadPool,
        @Valid ThreadPool smsThreadPool,
        @Valid ThreadPool phoneVerificationThreadPool,
        @Valid ThreadPool emailVerificationThreadPool,
        @Valid CircuitBreaker circuitBreaker
) {
    public record ThreadPool(
            @Min(1) @DefaultValue("6") int poolSize,
            @Min(1) @DefaultValue("20") int queueCapacity
    ) {}

    public record CircuitBreaker(
            @Min(1) @Max(100) @DefaultValue("50") float failureRateThreshold,
            @Min(1) @DefaultValue("20") int slidingWindowSize,
            @Min(1) @DefaultValue("10") int minimumNumberOfCalls,
            @Min(1) @DefaultValue("30") long waitDurationOpenSeconds,
            @Min(1) @DefaultValue("3") int permittedCallsInHalfOpenState
    ) {
        public CircuitBreaker {
            if (minimumNumberOfCalls > slidingWindowSize) {
                throw new IllegalArgumentException(
                        "알림 circuit breaker의 minimum-number-of-calls는 sliding-window-size 이하여야 합니다.");
            }
        }
    }
}
