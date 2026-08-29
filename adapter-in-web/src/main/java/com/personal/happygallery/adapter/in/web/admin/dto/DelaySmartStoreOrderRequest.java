package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.DelayCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record DelaySmartStoreOrderRequest(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime dispatchDueDate,
        @NotBlank
        @Pattern(regexp = "^(PRODUCT_PREPARE|CUSTOMER_REQUEST|CUSTOM_BUILD|RESERVED_DISPATCH|OVERSEA_DELIVERY|ETC)$")
        String reasonCode,
        @NotBlank @Size(max = 4000) String detailedReason
) {
    public DelayCommand toCommand(String productOrderId) {
        return new DelayCommand(productOrderId, dispatchDueDate, reasonCode, detailedReason);
    }
}
