package com.personal.happygallery.infra.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.external.payment")
public record ExternalPaymentProperties(
        @DefaultValue("3000") long timeoutMillis,
        CircuitBreaker circuitBreaker
) {
    public record CircuitBreaker(
            @DefaultValue("50") float failureRateThreshold,
            @DefaultValue("20") int slidingWindowSize,
            @DefaultValue("10") int minimumNumberOfCalls,
            @DefaultValue("30") long waitDurationOpenSeconds,
            @DefaultValue("3") int permittedCallsInHalfOpenState
    ) {}
}
