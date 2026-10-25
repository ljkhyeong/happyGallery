package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.application.cart.port.in.CartUseCase.MergeItem;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderItemRef;
import com.personal.happygallery.application.product.ProductOptions.TextInput;
import com.personal.happygallery.domain.order.OrderAmountCalculator;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 장바구니 병합과 주문 결제 payload가 함께 쓰는 상품 선택(상품·옵션 조합·텍스트 입력·수량) 요청. */
public record ProductSelectionRequest(
        @NotNull @Positive Long productId,
        @Positive @Schema(nullable = true) Long productVariantId,
        @Size(max = 5) List<@NotNull @Valid ProductTextInputRequest> textInputs,
        @Min(1) @Max(OrderAmountCalculator.MAX_ITEM_QUANTITY)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        int qty) {
    public ProductSelectionRequest {
        textInputs = textInputs == null ? List.of() : List.copyOf(textInputs);
    }

    public MergeItem toMergeItem() {
        return new MergeItem(productId, productVariantId, textInputCommands(), qty);
    }

    public OrderItemRef toOrderItemRef() {
        return new OrderItemRef(productId, productVariantId, textInputCommands(), qty);
    }

    private List<TextInput> textInputCommands() {
        return textInputs.stream().map(ProductTextInputRequest::toCommand).toList();
    }
}
