package com.personal.happygallery.adapter.in.web.config.properties;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("false") boolean trustForwardedHeaders,
        @Min(1) @DefaultValue("10") long phoneVerificationPerSecond,
        @Min(1) @DefaultValue("30") long bookingCreatePerMinute,
        @Min(1) @DefaultValue("20") long passPurchasePerMinute,
        @Min(1) @DefaultValue("10") long customerLoginPerMinute,
        @Min(1) @DefaultValue("5") long customerSignupPerMinute,
        @Min(1) @DefaultValue("5") long adminLoginPerMinute,
        @Min(1) @DefaultValue("5") long adminSetupPerMinute,
        @Min(1) @DefaultValue("120") long adminApiPerMinute,
        @Min(1) @DefaultValue("10") long socialLoginPerMinute
) {}
