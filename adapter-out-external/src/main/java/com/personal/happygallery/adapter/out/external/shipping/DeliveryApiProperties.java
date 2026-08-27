package com.personal.happygallery.adapter.out.external.shipping;

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
@ConfigurationProperties(prefix = "app.external.delivery-tracking")
public record DeliveryApiProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("") String apiKey,
        @DefaultValue("") String secretKey,
        @DefaultValue("") String webhookEndpointId,
        @DefaultValue("") String webhookSecret,
        @NotBlank @DefaultValue("https://api.deliveryapi.co.kr") String baseUrl,
        @NotNull @DurationMin(millis = 1) @DefaultValue("3s") Duration timeout,
        @NotNull @DurationMin(millis = 1) @DefaultValue("1s") Duration connectTimeout,
        @NotNull @DurationMin(millis = 1) @DefaultValue("500ms") Duration acquireTimeout,
        @Min(1) @DefaultValue("10") int maxConnections,
        @NotNull @DurationMin(millis = 1) @DefaultValue("30s") Duration keepAlive
) implements HttpPoolProperties {

    public DeliveryApiProperties {
        ExternalBaseUrl.requireHttpsOrigin(baseUrl, "배송조회");
        if (enabled && (!StringUtils.hasText(apiKey)
                || !StringUtils.hasText(secretKey)
                || !StringUtils.hasText(webhookEndpointId)
                || !StringUtils.hasText(webhookSecret))) {
            throw new IllegalArgumentException("배송조회 연동을 사용하려면 API 키와 웹훅 설정이 필요합니다.");
        }
    }
}
