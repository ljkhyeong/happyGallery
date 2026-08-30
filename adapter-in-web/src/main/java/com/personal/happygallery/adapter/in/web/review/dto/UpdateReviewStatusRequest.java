package com.personal.happygallery.adapter.in.web.review.dto;

import com.personal.happygallery.domain.review.Review;
import com.personal.happygallery.domain.review.ReviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateReviewStatusRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = ReviewStatus.class)
        @NotNull ReviewStatus status,
        @Schema(
                nullable = true,
                maxLength = Review.MAX_HIDDEN_REASON_LENGTH,
                description = "HIDDEN 전환 시 필수이며 PUBLISHED 전환 시 무시됩니다.")
        @Size(max = Review.MAX_HIDDEN_REASON_LENGTH) String reason,
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "1",
                description = "관리자가 확인한 후기 본문·평점·사진의 revision")
        @NotNull @Positive Long expectedContentRevision,
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "0",
                description = "관리자가 확인한 후기 운영 상태의 JPA version")
        @NotNull @PositiveOrZero Long expectedVersion
) {
}
