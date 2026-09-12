package com.personal.happygallery.adapter.out.external.shipping;

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
@ConfigurationProperties(prefix = "app.external.korea-post")
public record KoreaPostProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("") String serviceKey,
        @NotNull @DurationMin(millis = 1) @DefaultValue("5s") Duration timeout,
        @NotNull @DurationMin(millis = 1) @DefaultValue("1s") Duration connectTimeout,
        @NotNull @DurationMin(millis = 1) @DefaultValue("500ms") Duration acquireTimeout,
        @Min(1) @DefaultValue("5") int maxConnections,
        @NotNull @DurationMin(millis = 1) @DefaultValue("30s") Duration keepAlive
) implements HttpPoolProperties {
    public KoreaPostProperties {
        if (enabled && !StringUtils.hasText(serviceKey)) {
            throw new IllegalArgumentException("우체국 배송조회를 사용하려면 공공데이터포털 서비스키가 필요합니다.");
        }
    }
}
