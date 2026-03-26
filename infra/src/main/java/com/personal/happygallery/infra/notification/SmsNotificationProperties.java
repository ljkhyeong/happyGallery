package com.personal.happygallery.infra.notification;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.external.sms")
public record SmsNotificationProperties(
        @NotBlank String apiKey,
        @NotBlank String apiSecret,
        @NotBlank String senderNumber,
        @NotBlank @DefaultValue("https://api-sms.cloud.toast.com") String baseUrl,
        @Min(1) @DefaultValue("5000") long timeoutMillis
) {}
