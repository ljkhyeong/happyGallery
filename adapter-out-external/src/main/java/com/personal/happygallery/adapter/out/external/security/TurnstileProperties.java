package com.personal.happygallery.adapter.out.external.security;

import com.personal.happygallery.adapter.out.external.http.HttpPoolProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.external.turnstile")
public record TurnstileProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("") String siteKey,
        @DefaultValue("") String secretKey,
        @DefaultValue("happy-gallery.com") String hostname,
        @NotNull @DurationMin(millis = 1) @DefaultValue("5s") Duration timeout,
        @NotNull @DurationMin(millis = 1) @DefaultValue("1s") Duration connectTimeout,
        @NotNull @DurationMin(millis = 1) @DefaultValue("500ms") Duration acquireTimeout,
        @Min(1) @DefaultValue("5") int maxConnections,
        @NotNull @DurationMin(millis = 1) @DefaultValue("30s") Duration keepAlive
) implements HttpPoolProperties {
    public TurnstileProperties {
        if (enabled && (!StringUtils.hasText(siteKey) || !StringUtils.hasText(secretKey)
                || !StringUtils.hasText(hostname))) {
            throw new IllegalArgumentException("자동 입력 방지를 사용하려면 Turnstile 공개 키·비밀 키·도메인이 필요합니다.");
        }
    }
}
