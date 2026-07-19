package com.personal.happygallery.adapter.out.external.notification;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.external.notification")
public record NotificationResilienceProperties(
        @Min(1) @DefaultValue("3000") long timeoutMillis,
        @Valid ThreadPool threadPool,
        @Valid CircuitBreaker circuitBreaker
) {
    public record ThreadPool(
            @Min(1) @DefaultValue("6") int poolSize,
            @Min(1) @DefaultValue("20") int queueCapacity
    ) {}

    public record CircuitBreaker(
            @Min(1) @DefaultValue("50") float failureRateThreshold,
            @Min(1) @DefaultValue("20") int slidingWindowSize,
            @Min(1) @DefaultValue("10") int minimumNumberOfCalls,
            @Min(1) @DefaultValue("30") long waitDurationOpenSeconds,
            @Min(1) @DefaultValue("3") int permittedCallsInHalfOpenState
    ) {}
}
