package com.personal.happygallery.adapter.out.external.address;

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
@ConfigurationProperties(prefix = "app.external.road-address")
public record RoadAddressProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("") String confirmationKey,
        @NotBlank @DefaultValue("https://business.juso.go.kr") String baseUrl,
        @NotNull @DurationMin(millis = 1) @DefaultValue("3s") Duration timeout,
        @NotNull @DurationMin(millis = 1) @DefaultValue("1s") Duration connectTimeout,
        @NotNull @DurationMin(millis = 1) @DefaultValue("500ms") Duration acquireTimeout,
        @Min(1) @DefaultValue("10") int maxConnections,
        @NotNull @DurationMin(millis = 1) @DefaultValue("30s") Duration keepAlive
) implements HttpPoolProperties {

    public RoadAddressProperties {
        ExternalBaseUrl.requireHttpsOrigin(baseUrl, "도로명주소");
        if (enabled && !StringUtils.hasText(confirmationKey)) {
            throw new IllegalArgumentException("도로명주소 연동을 사용하려면 승인키가 필요합니다.");
        }
    }
}
