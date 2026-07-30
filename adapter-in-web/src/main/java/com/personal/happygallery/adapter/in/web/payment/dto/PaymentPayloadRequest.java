package com.personal.happygallery.adapter.in.web.payment.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.personal.happygallery.application.payment.port.in.PaymentPayload;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = OrderPaymentPayloadRequest.class, name = "ORDER"),
        @JsonSubTypes.Type(value = BookingPaymentPayloadRequest.class, name = "BOOKING"),
        @JsonSubTypes.Type(value = PassPaymentPayloadRequest.class, name = "PASS")
})
@Schema(
        name = "PaymentPayload",
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "ORDER", schema = OrderPaymentPayloadRequest.class),
                @DiscriminatorMapping(value = "BOOKING", schema = BookingPaymentPayloadRequest.class),
                @DiscriminatorMapping(value = "PASS", schema = PassPaymentPayloadRequest.class)
        })
public sealed interface PaymentPayloadRequest
        permits OrderPaymentPayloadRequest, BookingPaymentPayloadRequest, PassPaymentPayloadRequest {

    PaymentPayload toCommand();
}
