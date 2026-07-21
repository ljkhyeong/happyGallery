package com.personal.happygallery.application.token;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.guest-token")
public record GuestTokenProperties(
        @NotBlank @Size(min = 32) String hmacSecret,
        @DefaultValue("") String previousHmacSecret,
        @Min(1) @DefaultValue("168") long expiryHours,
        @Min(1) @DefaultValue("24") long recoveryExpiryHours
) {

    public GuestTokenProperties {
        if (previousHmacSecret == null || previousHmacSecret.isBlank()) {
            previousHmacSecret = "";
        } else if (previousHmacSecret.length() < 32) {
            throw new IllegalArgumentException("이전 게스트 토큰 HMAC 키는 32자 이상이어야 합니다.");
        }
        if (hmacSecret != null && hmacSecret.equals(previousHmacSecret)) {
            throw new IllegalArgumentException("활성 키와 이전 게스트 토큰 HMAC 키는 달라야 합니다.");
        }
    }
}
