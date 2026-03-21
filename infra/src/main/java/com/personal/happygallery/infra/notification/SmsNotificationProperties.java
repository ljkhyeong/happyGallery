package com.personal.happygallery.infra.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.external.sms")
public record SmsNotificationProperties(
        String apiKey,
        String apiSecret,
        String senderNumber,
        @DefaultValue("https://api-sms.cloud.toast.com") String baseUrl,
        @DefaultValue("5000") long timeoutMillis
) {}
