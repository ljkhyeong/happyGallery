package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.dashboard.dto.StatusCount;
import com.personal.happygallery.domain.order.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record StatusCountResponse(
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                implementation = OrderStatus.class)
        String status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int count
) {

    public static StatusCountResponse from(StatusCount statusCount) {
        return new StatusCountResponse(statusCount.status(), statusCount.count());
    }
}
