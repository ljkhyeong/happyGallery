package com.personal.happygallery.adapter.out.external.shipping;

import com.personal.happygallery.adapter.out.external.http.ExternalBaseUrl;
import com.personal.happygallery.adapter.out.external.http.HttpPool;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
        @Valid @DefaultValue HttpPool http
) {

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
