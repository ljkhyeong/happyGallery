package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.ReturnDeliveryCompanyResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record SmartStoreReturnDeliveryCompanyResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String priorityType
) {
    public static SmartStoreReturnDeliveryCompanyResponse from(ReturnDeliveryCompanyResult result) {
        return new SmartStoreReturnDeliveryCompanyResponse(result.id(), result.name(), result.priorityType());
    }
}
