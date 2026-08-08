package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.event.port.in.EventAdminUseCase.UpdateCommand;
import com.personal.happygallery.domain.event.Event;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Set;

public record UpdateEventRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @PositiveOrZero
        Long expectedVersion,
        @NotBlank
        @Size(max = Event.MAX_TITLE_LENGTH)
        String title,
        @NotBlank
        @Size(max = Event.MAX_SUMMARY_LENGTH)
        String summary,
        @NotBlank
        @Size(max = Event.MAX_CONTENT_LENGTH)
        String content,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        @Size(max = Event.MAX_IMAGE_URL_LENGTH)
        String imageUrl,
        @NotNull
        LocalDateTime startAt,
        @NotNull
        LocalDateTime endAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        boolean published,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        boolean featured,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        Set<@Positive Long> relatedProductIds
) {
    public UpdateCommand toCommand() {
        return new UpdateCommand(
                expectedVersion,
                title,
                summary,
                content,
                imageUrl,
                startAt,
                endAt,
                published,
                featured,
                relatedProductIds);
    }
}
