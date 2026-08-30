package com.personal.happygallery.application.token;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.guest-token")
public record GuestTokenProperties(
        @NotBlank @Size(min = 32) String hmacSecret,
        @Pattern(
                regexp = "(?s)^$|.{32,}$",
                message = "이전 게스트 토큰 HMAC 키는 32자 이상이어야 합니다.")
        @DefaultValue("") String previousHmacSecret,
        @NotNull @DurationMin(hours = 1) @DefaultValue("720h") Duration accessExpiry,
        @NotNull @DurationMin(hours = 1) @DefaultValue("24h") Duration recoveryExpiry
) {

    public GuestTokenProperties {
        if (previousHmacSecret == null || previousHmacSecret.isBlank()) {
            previousHmacSecret = "";
        }
        if (hmacSecret != null && !hmacSecret.isBlank() && hmacSecret.equals(previousHmacSecret)) {
            throw new IllegalArgumentException("활성 키와 이전 게스트 토큰 HMAC 키는 달라야 합니다.");
        }
    }
}
