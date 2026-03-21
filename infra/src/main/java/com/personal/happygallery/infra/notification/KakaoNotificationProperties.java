package com.personal.happygallery.infra.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.external.kakao")
public record KakaoNotificationProperties(
        String apiKey,
        String senderKey,
        @DefaultValue("https://bizapi.kakao.com") String baseUrl,
        @DefaultValue("5000") long timeoutMillis
) {}
