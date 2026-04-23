package com.personal.happygallery.adapter.out.external.payment;

import com.personal.happygallery.adapter.out.external.http.HttpPoolProperties;
import jakarta.validation.constraints.Min;
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
        @Min(1) @DefaultValue("5000") long timeoutMillis,
        @Min(1) @DefaultValue("2000") long connectTimeoutMillis,
        @Min(1) @DefaultValue("1000") long acquireTimeoutMillis,
        @Min(1) @DefaultValue("10") int maxConnections,
        @Min(1) @DefaultValue("30000") long keepAliveMillis
) implements HttpPoolProperties {}
