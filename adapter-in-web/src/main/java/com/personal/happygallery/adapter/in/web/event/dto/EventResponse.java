package com.personal.happygallery.adapter.in.web.event.dto;

import com.personal.happygallery.domain.event.Event;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record EventResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String summary,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String imageUrl,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime startAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime endAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean published,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean featured,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long couponDefinitionId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Long> relatedProductIds,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long version
) {
    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getSummary(),
                event.getContent(),
                event.getImageUrl(),
                event.getStartAt(),
                event.getEndAt(),
                event.isPublished(),
                event.isFeatured(),
                event.getCouponDefinitionId(),
                event.getRelatedProductIds().stream().sorted().toList(),
                event.getVersion());
    }
}
