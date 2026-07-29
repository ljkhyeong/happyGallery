package com.personal.happygallery.adapter.in.web.booking.dto;

import com.personal.happygallery.domain.booking.Slot;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record PublicSlotResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long classId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime startAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime endAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int capacity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int bookedCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int remainingCapacity
) {
    public static PublicSlotResponse from(Slot slot) {
        return new PublicSlotResponse(
                slot.getId(),
                slot.getBookingClass().getId(),
                slot.getStartAt(),
                slot.getEndAt(),
                slot.getCapacity(),
                slot.getBookedCount(),
                slot.getCapacity() - slot.getBookedCount()
        );
    }
}
