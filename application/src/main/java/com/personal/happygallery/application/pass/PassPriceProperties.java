package com.personal.happygallery.application.pass;

import com.personal.happygallery.domain.payment.PaymentAmountPolicy;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 8회권 단일 가격 (원, KRW). 클라이언트가 금액을 전송하지 않으므로 서버에서 주입.
 *
 * <p>운영 환경 변수: {@code PASS_TOTAL_PRICE} / 기본값 240000.
 */
@Validated
@ConfigurationProperties(prefix = "app.pass")
public record PassPriceProperties(
        @Positive
        @Max(PaymentAmountPolicy.MAX_AMOUNT)
        long totalPrice
) {}
