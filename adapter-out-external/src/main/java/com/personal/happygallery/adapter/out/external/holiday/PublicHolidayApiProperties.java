package com.personal.happygallery.adapter.out.external.holiday;

import com.personal.happygallery.adapter.out.external.http.ExternalBaseUrl;
import com.personal.happygallery.adapter.out.external.http.HttpPoolProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.external.public-holiday")
public record PublicHolidayApiProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("") String serviceKey,
        @NotBlank @DefaultValue("https://apis.data.go.kr") String baseUrl,
        @NotNull @DurationMin(millis = 1) @DefaultValue("5s") Duration timeout,
        @NotNull @DurationMin(millis = 1) @DefaultValue("1s") Duration connectTimeout,
        @NotNull @DurationMin(millis = 1) @DefaultValue("500ms") Duration acquireTimeout,
        @Min(1) @DefaultValue("5") int maxConnections,
        @NotNull @DurationMin(millis = 1) @DefaultValue("30s") Duration keepAlive
) implements HttpPoolProperties {

    public PublicHolidayApiProperties {
        ExternalBaseUrl.requireHttpsOrigin(baseUrl, "공공데이터 공휴일");
        if (enabled && !StringUtils.hasText(serviceKey)) {
            throw new IllegalArgumentException("공휴일 연동을 사용하려면 공공데이터 서비스키가 필요합니다.");
        }
    }
}
