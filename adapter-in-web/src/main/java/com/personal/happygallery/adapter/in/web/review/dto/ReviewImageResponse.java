package com.personal.happygallery.adapter.in.web.review.dto;

import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewImageItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record ReviewImageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String imageUrl,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0", maximum = "4")
        int sortOrder,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt
) {
    public static ReviewImageResponse from(ReviewImageItem image) {
        return new ReviewImageResponse(
                image.id(), image.imageUrl(), image.sortOrder(), image.createdAt());
    }
}
