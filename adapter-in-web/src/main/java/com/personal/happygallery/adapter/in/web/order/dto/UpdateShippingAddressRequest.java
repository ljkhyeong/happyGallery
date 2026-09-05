package com.personal.happygallery.adapter.in.web.order.dto;

import com.personal.happygallery.adapter.in.web.payment.dto.OrderPaymentPayloadRequest.ShippingAddressRequest;
import com.personal.happygallery.domain.order.ShippingAddress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateShippingAddressRequest(
        @NotNull @PositiveOrZero Long version,
        @NotNull @Valid ShippingAddressRequest shippingAddress) {
    public ShippingAddress toAddress() {
        return new ShippingAddress(shippingAddress.recipientName(), shippingAddress.phone(),
                shippingAddress.postalCode(), shippingAddress.addressLine1(), shippingAddress.addressLine2());
    }
}
