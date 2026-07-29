package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.BookingClassStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record AdminClassResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String category,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int durationMin,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long price,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int bufferMin,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean passEligible,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String description,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String imageUrl,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String preparationInfo,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String targetAudience,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BookingClassStatus status
) {

    public static AdminClassResponse from(BookingClass bookingClass) {
        return new AdminClassResponse(
                bookingClass.getId(),
                bookingClass.getName(),
                bookingClass.getCategory(),
                bookingClass.getDurationMin(),
                bookingClass.getPrice(),
                bookingClass.getBufferMin(),
                bookingClass.isPassEligible(),
                bookingClass.getDescription(),
                bookingClass.getImageUrl(),
                bookingClass.getPreparationInfo(),
                bookingClass.getTargetAudience(),
                bookingClass.getStatus());
    }
}
