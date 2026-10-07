package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.adapter.out.external.resilience.ExecutorPoolProperties;
import com.personal.happygallery.adapter.out.external.resilience.ExternalCircuitBreakerProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.external.notification")
public record NotificationResilienceProperties(
        @NotNull @DurationMin(millis = 1) @DefaultValue("5s") Duration timeout,
        @Valid ExecutorPoolProperties alimtalkThreadPool,
        @Valid ExecutorPoolProperties smsThreadPool,
        @Valid ExecutorPoolProperties phoneVerificationThreadPool,
        @Valid ExecutorPoolProperties emailVerificationThreadPool,
        @Valid ExternalCircuitBreakerProperties circuitBreaker
) {}
