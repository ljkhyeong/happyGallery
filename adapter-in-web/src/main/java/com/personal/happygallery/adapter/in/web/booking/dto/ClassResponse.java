package com.personal.happygallery.adapter.in.web.booking.dto;

import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.BookingClassStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record ClassResponse(
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
