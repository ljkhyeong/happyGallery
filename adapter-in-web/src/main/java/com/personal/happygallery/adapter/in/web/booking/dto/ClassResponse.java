package com.personal.happygallery.adapter.in.web.booking.dto;

import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.BookingClassStatus;

public record ClassResponse(
        Long id,
        String name,
        String category,
        int durationMin,
        long price,
        int bufferMin,
        boolean passEligible,
        String description,
        String imageUrl,
        String preparationInfo,
        String targetAudience,
        BookingClassStatus status
) {
    public static ClassResponse from(BookingClass bc) {
        return new ClassResponse(
                bc.getId(),
                bc.getName(),
                bc.getCategory(),
                bc.getDurationMin(),
                bc.getPrice(),
                bc.getBufferMin(),
                bc.isPassEligible(),
                bc.getDescription(),
                bc.getImageUrl(),
                bc.getPreparationInfo(),
                bc.getTargetAudience(),
                bc.getStatus()
        );
    }
}
