package com.personal.happygallery.adapter.out.external.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.function.Predicate;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** 외부 연동별 CircuitBreaker에 공통으로 적용하는 설정 값. */
public record ExternalCircuitBreakerProperties(
        @Min(1) @Max(100) @DefaultValue("50") float failureRateThreshold,
        @Min(1) @DefaultValue("20") int slidingWindowSize,
        @Min(1) @DefaultValue("10") int minimumNumberOfCalls,
        @NotNull @DurationMin(seconds = 1) @DefaultValue("30s") Duration waitDurationOpen,
        @Min(1) @DefaultValue("3") int permittedCallsInHalfOpenState
) {
    /** 공통 임계값에 연동별 실패 결과 판정을 더해 CircuitBreaker 설정을 만든다. */
    public CircuitBreakerConfig toConfig(Predicate<Object> recordResult) {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(failureRateThreshold)
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(minimumNumberOfCalls)
                .waitDurationInOpenState(waitDurationOpen)
                .permittedNumberOfCallsInHalfOpenState(permittedCallsInHalfOpenState)
                .recordResult(recordResult)
                .build();
    }

    @AssertTrue(message = "minimum-number-of-calls는 sliding-window-size 이하여야 합니다.")
    public boolean isMinimumNumberOfCallsWithinWindow() {
        return minimumNumberOfCalls <= slidingWindowSize;
    }
}
