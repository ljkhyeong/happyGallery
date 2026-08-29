package com.personal.happygallery.adapter.out.external.smartstore;

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
@ConfigurationProperties(prefix = "app.external.smartstore")
public record SmartStoreProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("") String clientId,
        @DefaultValue("") String clientSecret,
        @DefaultValue("SELF") String accountType,
        @DefaultValue("") String accountId,
        @NotBlank @DefaultValue("https://api.commerce.naver.com") String baseUrl,
        @NotNull @DurationMin(millis = 1) @DefaultValue("5s") Duration timeout,
        @NotNull @DurationMin(millis = 1) @DefaultValue("1s") Duration connectTimeout,
        @NotNull @DurationMin(millis = 1) @DefaultValue("500ms") Duration acquireTimeout,
        @Min(1) @DefaultValue("5") int maxConnections,
        @NotNull @DurationMin(millis = 1) @DefaultValue("30s") Duration keepAlive
) implements HttpPoolProperties {

    public SmartStoreProperties {
        ExternalBaseUrl.requireHttpsOrigin(baseUrl, "스마트스토어");
        if (!accountType.equals("SELF") && !accountType.equals("SELLER")) {
            throw new IllegalArgumentException("스마트스토어 계정 유형은 SELF 또는 SELLER여야 합니다.");
        }
        if (enabled && (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret))) {
            throw new IllegalArgumentException("스마트스토어 재고 연동을 사용하려면 클라이언트 인증 정보가 필요합니다.");
        }
        if (enabled && accountType.equals("SELLER") && !StringUtils.hasText(accountId)) {
            throw new IllegalArgumentException("SELLER 방식 스마트스토어 연동에는 판매자 계정 ID가 필요합니다.");
        }
    }
}
