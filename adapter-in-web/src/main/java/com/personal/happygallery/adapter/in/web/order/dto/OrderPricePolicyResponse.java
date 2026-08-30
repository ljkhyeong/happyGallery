package com.personal.happygallery.adapter.in.web.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record OrderPricePolicyResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long shippingFee,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String madeToOrderConsentVersion,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String madeToOrderConsentText
) {}
