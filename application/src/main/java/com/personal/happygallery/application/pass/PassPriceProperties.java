package com.personal.happygallery.application.pass;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 8회권 단일 가격 (원, KRW). 클라이언트가 금액을 전송하지 않으므로 서버에서 주입.
 *
 * <p>운영 환경 변수: {@code PASS_TOTAL_PRICE} / 기본값 240000.
 */
@ConfigurationProperties(prefix = "app.pass")
public record PassPriceProperties(long totalPrice) {

    public PassPriceProperties {
        if (totalPrice <= 0) {
            throw new IllegalArgumentException("PASS_TOTAL_PRICE는 양수여야 합니다.");
        }
    }
}
