package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.DispatchCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record DispatchSmartStoreOrderRequest(
        @NotBlank @Pattern(regexp = "^[A-Z_]{1,40}$") String deliveryMethod,
        @Pattern(regexp = "^[A-Z0-9_]{1,40}$") String deliveryCompanyCode,
        @Size(max = 100) String trackingNumber,
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime dispatchDate
) {
    public DispatchCommand toCommand(String productOrderId) {
        return new DispatchCommand(
                productOrderId, deliveryMethod, deliveryCompanyCode, trackingNumber, dispatchDate);
    }
}
