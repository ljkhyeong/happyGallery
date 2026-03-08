package com.personal.happygallery.app.web.booking.dto;

import com.personal.happygallery.domain.booking.Slot;
import java.time.LocalDateTime;

public record PublicSlotResponse(
        Long id,
        Long classId,
        LocalDateTime startAt,
        LocalDateTime endAt,
        int capacity,
        int bookedCount,
        int remainingCapacity
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
