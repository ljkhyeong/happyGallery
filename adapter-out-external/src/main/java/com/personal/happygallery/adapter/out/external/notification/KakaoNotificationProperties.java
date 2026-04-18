package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.adapter.out.external.http.HttpPoolProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.external.kakao")
public record KakaoNotificationProperties(
        @NotBlank String apiKey,
        @NotBlank String senderKey,
        @NotBlank @DefaultValue("https://bizapi.kakao.com") String baseUrl,
        @Min(1) @DefaultValue("5000") long timeoutMillis,
        @Min(1) @DefaultValue("2000") long connectTimeoutMillis,
        @Min(1) @DefaultValue("1000") long acquireTimeoutMillis,
        @Min(1) @DefaultValue("20") int maxConnections,
        @Min(1) @DefaultValue("30000") long keepAliveMillis
) implements HttpPoolProperties {}
