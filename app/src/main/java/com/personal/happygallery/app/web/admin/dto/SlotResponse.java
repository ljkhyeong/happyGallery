package com.personal.happygallery.app.web.admin.dto;

import com.personal.happygallery.domain.booking.Slot;
import java.time.LocalDateTime;

public record SlotResponse(
        Long id,
        Long classId,
        LocalDateTime startAt,
        LocalDateTime endAt,
        int capacity,
        int bookedCount,
        boolean isActive
) {
    public static SlotResponse from(Slot slot) {
        return new SlotResponse(
                slot.getId(),
                slot.getBookingClass().getId(),
                slot.getStartAt(),
                slot.getEndAt(),
                slot.getCapacity(),
                slot.getBookedCount(),
                slot.isActive()
        );
    }
}
