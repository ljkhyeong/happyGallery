package com.personal.happygallery.application.order;

import com.personal.happygallery.domain.payment.PaymentAmountPolicy;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.order")
public record OrderPriceProperties(
        @PositiveOrZero
        @Max(PaymentAmountPolicy.MAX_AMOUNT)
        long shippingFee
) {}
