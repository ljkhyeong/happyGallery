package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.dashboard.dto.SlotUtilization;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record SlotUtilizationResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate date,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String className,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int totalCapacity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int totalBooked,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) double utilizationRate
) {

    public static SlotUtilizationResponse from(SlotUtilization utilization) {
        return new SlotUtilizationResponse(
                utilization.date(),
                utilization.className(),
                utilization.totalCapacity(),
                utilization.totalBooked(),
                utilization.utilizationRate()
        );
    }
}
