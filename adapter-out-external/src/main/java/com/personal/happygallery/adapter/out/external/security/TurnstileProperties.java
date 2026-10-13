package com.personal.happygallery.adapter.out.external.security;

import com.personal.happygallery.adapter.out.external.http.HttpPool;
import jakarta.validation.Valid;
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
        @Valid @DefaultValue HttpPool http
) {
    public TurnstileProperties {
        if (enabled && (!StringUtils.hasText(siteKey) || !StringUtils.hasText(secretKey)
                || !StringUtils.hasText(hostname))) {
            throw new IllegalArgumentException("자동 입력 방지를 사용하려면 Turnstile 공개 키·비밀 키·도메인이 필요합니다.");
        }
    }
}
