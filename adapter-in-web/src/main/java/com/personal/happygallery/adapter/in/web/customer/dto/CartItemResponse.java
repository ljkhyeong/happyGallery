package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.domain.product.ProductType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record CartItemResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long cartItemId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long productId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long productVariantId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String productName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ProductType productType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long basePrice,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long variantPriceAdjustment,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long textOptionPriceAdjustment,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long price,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String specification,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String careInstructions,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Integer productionLeadDays,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<ProductOptionSnapshotResponse> options,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int qty,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long subtotal,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean available,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0",
                description = "같은 상품·옵션 조합에서 구매 가능한 현재 수량. 판매 중지·변경된 옵션은 0. 같은 조합의 선택 수량 합계를 이 값과 비교한다")
        int availableQuantity
) {}
