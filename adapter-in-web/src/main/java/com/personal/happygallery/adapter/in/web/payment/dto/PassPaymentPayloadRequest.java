package com.personal.happygallery.adapter.in.web.payment.dto;

import com.personal.happygallery.application.payment.port.in.PaymentPayload;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(name = "PassPayload")
public record PassPaymentPayloadRequest(
        @NotBlank
        @Pattern(regexp = "PASS")
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = "PASS")
        String type,
        @NotNull Long userId
) implements PaymentPayloadRequest {

    @Override
    public PaymentPayload.PassPayload toCommand() {
        return new PaymentPayload.PassPayload(userId);
    }
}
