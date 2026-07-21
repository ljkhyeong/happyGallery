package com.personal.happygallery.adapter.in.web.order.dto;

public record OrderPricePolicyResponse(
        long shippingFee,
        String madeToOrderConsentVersion,
        String madeToOrderConsentText
) {}
