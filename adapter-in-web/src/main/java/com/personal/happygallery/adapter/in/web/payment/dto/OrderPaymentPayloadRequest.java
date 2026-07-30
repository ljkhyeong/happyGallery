package com.personal.happygallery.adapter.in.web.payment.dto;

import com.personal.happygallery.adapter.in.web.policy.dto.PolicyAcceptanceRequest;
import com.personal.happygallery.application.payment.port.in.PaymentPayload;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.OrderAmountCalculator;
import com.personal.happygallery.domain.order.ShippingAddress;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.util.List;

@Schema(name = "OrderPayload")
public record OrderPaymentPayloadRequest(
        @NotBlank
        @Pattern(regexp = "ORDER")
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = "ORDER")
        String type,
        @Schema(nullable = true) Long userId,
        @Schema(nullable = true) String phone,
        @Schema(nullable = true) String verificationCode,
        @Schema(nullable = true) String name,
        @NotNull List<@Valid OrderItemRefRequest> items,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean cartCheckout,
        @NotNull FulfillmentType fulfillmentType,
        @Valid @Schema(nullable = true) ShippingAddressRequest shippingAddress,
        @Schema(nullable = true) String madeToOrderConsentVersion,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean madeToOrderConsent,
        @Valid @Schema(nullable = true) PolicyAcceptanceRequest policyAcceptance
) implements PaymentPayloadRequest {

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
                policyAcceptance == null ? null : policyAcceptance.toCommand());
    }

    @Schema(name = "OrderItemRef")
    public record OrderItemRefRequest(
            @NotNull @Positive Long productId,
            @Min(1)
            @Max(OrderAmountCalculator.MAX_ITEM_QUANTITY)
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            int qty
    ) {
        private PaymentPayload.OrderItemRef toCommand() {
            return new PaymentPayload.OrderItemRef(productId, qty);
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
