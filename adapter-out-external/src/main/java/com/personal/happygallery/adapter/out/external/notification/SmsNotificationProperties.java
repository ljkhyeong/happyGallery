package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.adapter.out.external.http.HttpPoolProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.external.sms")
public record SmsNotificationProperties(
        @NotBlank String apiKey,
        @NotBlank String apiSecret,
        @NotBlank String senderNumber,
        @NotBlank @DefaultValue("https://sms.api.nhncloudservice.com") String baseUrl,
        @Min(1) @DefaultValue("2000") long timeoutMillis,
        @Min(1) @DefaultValue("1000") long connectTimeoutMillis,
        @Min(1) @DefaultValue("500") long acquireTimeoutMillis,
        @Min(1) @DefaultValue("20") int maxConnections,
        @Min(1) @DefaultValue("30000") long keepAliveMillis
) implements HttpPoolProperties {

    public SmsNotificationProperties {
        validateBaseUrl(baseUrl);
    }

    private static void validateBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("SMS base URL 형식이 올바르지 않습니다.", exception);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null
                || !(uri.getPath().isEmpty() || "/".equals(uri.getPath()))) {
            throw new IllegalArgumentException("SMS base URL은 경로가 없는 HTTPS 주소여야 합니다.");
        }
    }
}
