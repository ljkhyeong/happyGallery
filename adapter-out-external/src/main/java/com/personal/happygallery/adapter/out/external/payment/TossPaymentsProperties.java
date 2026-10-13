package com.personal.happygallery.adapter.out.external.payment;

import com.personal.happygallery.adapter.out.external.http.ExternalBaseUrl;
import com.personal.happygallery.adapter.out.external.http.HttpPool;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Toss Payments 연동 설정.
 * secretKey는 {@code TOSS_SECRET_KEY} 환경 변수로 prod 프로필에서만 주입.
 * local/test에서는 비어 있어도 기동 가능하도록 기본값 "".
 */
@Validated
@ConfigurationProperties(prefix = "app.external.payment.toss")
public record TossPaymentsProperties(
        @DefaultValue("") String secretKey,
        @NotBlank @DefaultValue("https://api.tosspayments.com") String baseUrl,
        @Valid @DefaultValue HttpPool http
) {

    public TossPaymentsProperties {
        ExternalBaseUrl.requireHttpsOrigin(baseUrl, "Toss Payments");
    }
}
