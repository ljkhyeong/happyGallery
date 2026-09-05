package com.personal.happygallery.adapter.in.web.payment.dto;

import com.personal.happygallery.adapter.in.web.policy.dto.PolicyAcceptanceRequest;
import com.personal.happygallery.application.payment.port.in.PaymentPayload;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.OrderAmountCalculator;
import com.personal.happygallery.domain.order.ShippingAddress;
import com.personal.happygallery.domain.payment.PaymentAmountPolicy;
import com.personal.happygallery.domain.product.ProductOptionPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Objects;

@Schema(name = "OrderPayload")
public record OrderPaymentPayloadRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = "ORDER")
        String type,
        @Schema(nullable = true) Long userId,
        @Schema(nullable = true) String phone,
        @Schema(nullable = true) String verificationCode,
        @Schema(nullable = true) String name,
        @NotNull @Size(max = 100) List<@NotNull @Valid OrderItemRefRequest> items,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean cartCheckout,
        @NotNull FulfillmentType fulfillmentType,
        @Valid @Schema(nullable = true) ShippingAddressRequest shippingAddress,
        @Schema(nullable = true) String madeToOrderConsentVersion,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean madeToOrderConsent,
        @Valid @Schema(nullable = true) PolicyAcceptanceRequest policyAcceptance,
        @Pattern(regexp = "^[0-9a-f]{64}$")
        @Schema(
                nullable = true,
                description = "GET /api/v1/me/cart가 반환한 불투명 장바구니 스냅샷 버전")
        String expectedCartVersion,
        @Positive @Schema(nullable = true) Long issuedCouponId,
        @PositiveOrZero
        @Max(PaymentAmountPolicy.MAX_AMOUNT)
        @Schema(
                nullable = true,
                defaultValue = "0",
                minimum = "0",
                maximum = "9007199254740991")
        Long rewardAmount,
        @Size(min = 1, max = 100)
        @Schema(nullable = true, description = "선택 구매할 본인 장바구니 행 ID. 생략 또는 null이면 구매 가능한 전체 항목. 지정 시 expectedCartVersion 필수")
        List<@NotNull @Positive Long> selectedCartItemIds
) implements PaymentPayloadRequest {

    public OrderPaymentPayloadRequest(String type, Long userId, String phone, String verificationCode,
            String name, List<OrderItemRefRequest> items, boolean cartCheckout,
            FulfillmentType fulfillmentType, ShippingAddressRequest shippingAddress,
            String madeToOrderConsentVersion, boolean madeToOrderConsent,
            PolicyAcceptanceRequest policyAcceptance, String expectedCartVersion,
            Long issuedCouponId, Long rewardAmount) {
        this(type, userId, phone, verificationCode, name, items, cartCheckout, fulfillmentType,
                shippingAddress, madeToOrderConsentVersion, madeToOrderConsent, policyAcceptance,
                expectedCartVersion, issuedCouponId, rewardAmount, null);
    }

    /** 혜택 요청 필드 도입 전 호출부 호환 생성자. */
    public OrderPaymentPayloadRequest(
            String type,
            Long userId,
            String phone,
            String verificationCode,
            String name,
            List<OrderItemRefRequest> items,
            boolean cartCheckout,
            FulfillmentType fulfillmentType,
            ShippingAddressRequest shippingAddress,
            String madeToOrderConsentVersion,
            boolean madeToOrderConsent,
            PolicyAcceptanceRequest policyAcceptance,
            String expectedCartVersion) {
        this(type, userId, phone, verificationCode, name, items, cartCheckout,
                fulfillmentType, shippingAddress, madeToOrderConsentVersion,
                madeToOrderConsent, policyAcceptance, expectedCartVersion, null, null);
    }

    @Override
    public PaymentPayload.OrderPayload toCommand() {
        return new PaymentPayload.OrderPayload(
                userId,
                phone,
                verificationCode,
                name,
                items.stream().map(OrderItemRefRequest::toCommand).toList(),
                cartCheckout,
                fulfillmentType,
                shippingAddress == null ? null : shippingAddress.toCommand(),
                madeToOrderConsentVersion,
                madeToOrderConsent,
                policyAcceptance == null ? null : policyAcceptance.toCommand(),
                expectedCartVersion,
                issuedCouponId,
                Objects.requireNonNullElse(rewardAmount, 0L),
                selectedCartItemIds);
    }

    @Schema(name = "OrderItemRef")
    public record OrderItemRefRequest(
            @NotNull @Positive Long productId,
            @Positive @Schema(nullable = true) Long productVariantId,
            @Size(max = 5) List<@NotNull @Valid OrderTextInputRequest> textInputs,
            @Min(1)
            @Max(OrderAmountCalculator.MAX_ITEM_QUANTITY)
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            int qty
    ) {
        public OrderItemRefRequest {
            textInputs = textInputs == null ? List.of() : List.copyOf(textInputs);
        }

        public OrderItemRefRequest(Long productId, int qty) {
            this(productId, null, List.of(), qty);
        }

        private PaymentPayload.OrderItemRef toCommand() {
            return new PaymentPayload.OrderItemRef(
                    productId,
                    productVariantId,
                    textInputs.stream().map(OrderTextInputRequest::toCommand).toList(),
                    qty);
        }
    }

    @Schema(name = "OrderTextInput")
    public record OrderTextInputRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{1,64}$") String groupKey,
            @Size(max = ProductOptionPolicy.MAX_INPUT_LENGTH) String value
    ) {
        private PaymentPayload.OrderTextInput toCommand() {
            return new PaymentPayload.OrderTextInput(groupKey, value);
        }
    }

    @Schema(name = "ShippingAddress")
    public record ShippingAddressRequest(
            @NotBlank String recipientName,
            @NotBlank String phone,
            @NotBlank @Pattern(regexp = "^[0-9]{5}$") String postalCode,
            @NotBlank String addressLine1,
            @Schema(nullable = true) String addressLine2
    ) {
        private ShippingAddress toCommand() {
            return new ShippingAddress(
                    recipientName, phone, postalCode, addressLine1, addressLine2);
        }
    }
}
