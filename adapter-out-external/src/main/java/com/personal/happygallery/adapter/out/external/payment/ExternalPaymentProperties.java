package com.personal.happygallery.adapter.out.external.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.external.payment")
public record ExternalPaymentProperties(
        @Min(1) @DefaultValue("3000") long timeoutMillis,
        @Valid CircuitBreaker circuitBreaker
) {
    public record CircuitBreaker(
            @Min(1) @DefaultValue("50") float failureRateThreshold,
            @Min(1) @DefaultValue("20") int slidingWindowSize,
            @Min(1) @DefaultValue("10") int minimumNumberOfCalls,
            @Min(1) @DefaultValue("30") long waitDurationOpenSeconds,
            @Min(1) @DefaultValue("3") int permittedCallsInHalfOpenState
    ) {}
}
