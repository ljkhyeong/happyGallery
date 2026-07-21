package com.personal.happygallery.application.order;

import com.personal.happygallery.domain.payment.PaymentAmountPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.order")
public record OrderPriceProperties(long shippingFee) {

    public OrderPriceProperties {
        if (shippingFee < 0L || shippingFee > PaymentAmountPolicy.MAX_AMOUNT) {
            throw new IllegalArgumentException("ORDER_SHIPPING_FEE는 0원 이상 허용 범위 이하여야 합니다.");
        }
    }
}
