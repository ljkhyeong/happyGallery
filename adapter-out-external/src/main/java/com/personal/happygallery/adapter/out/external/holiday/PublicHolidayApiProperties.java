package com.personal.happygallery.adapter.out.external.holiday;

import com.personal.happygallery.adapter.out.external.http.ExternalBaseUrl;
import com.personal.happygallery.adapter.out.external.http.HttpPool;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
        @Valid @DefaultValue HttpPool http
) {

    public PublicHolidayApiProperties {
        ExternalBaseUrl.requireHttpsOrigin(baseUrl, "공공데이터 공휴일");
        if (enabled && !StringUtils.hasText(serviceKey)) {
            throw new IllegalArgumentException("공휴일 연동을 사용하려면 공공데이터 서비스키가 필요합니다.");
        }
    }
}
