package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.booking.Slot;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record SlotResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long classId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime startAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime endAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int capacity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int bookedCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean adminActive,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean bufferBlocked,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean isActive
) {
    public static SlotResponse from(Slot slot) {
        return new SlotResponse(
                slot.getId(),
                slot.getBookingClass().getId(),
                slot.getStartAt(),
                slot.getEndAt(),
                slot.getCapacity(),
                slot.getBookedCount(),
                slot.isAdminActive(),
                slot.isBufferBlocked(),
                slot.isActive()
        );
    }
}
