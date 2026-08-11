package com.personal.happygallery.adapter.in.web.review.dto;

import com.personal.happygallery.domain.content.ContentTextPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateReviewRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1")
        @NotNull @Min(1) Long expectedContentRevision,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "5")
        @NotNull @Min(1) @Max(5) Integer rating,
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = ContentTextPolicy.MIN_LENGTH,
                maxLength = ContentTextPolicy.MAX_BODY_LENGTH)
        @NotBlank
        @Size(min = ContentTextPolicy.MIN_LENGTH, max = ContentTextPolicy.MAX_BODY_LENGTH)
        String content
) {
}
