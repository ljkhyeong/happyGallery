package com.personal.happygallery.adapter.in.web.review.dto;

import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewEvidenceItem;
import com.personal.happygallery.domain.review.ReviewEvidenceProvenance;
import com.personal.happygallery.domain.review.ReviewImage;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

public record ReviewEvidenceResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1") long contentRevision,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "5") int rating,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime editedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = ReviewEvidenceProvenance.class)
        ReviewEvidenceProvenance provenance,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean imagesComplete,
        @ArraySchema(
                arraySchema = @Schema(requiredMode = Schema.RequiredMode.REQUIRED),
                schema = @Schema(type = "string"),
                maxItems = ReviewImage.MAX_IMAGES)
        List<String> imageUrls,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime capturedAt
) {
    public static ReviewEvidenceResponse from(ReviewEvidenceItem evidence) {
        return evidence == null ? null : new ReviewEvidenceResponse(
                evidence.id(),
                evidence.contentRevision(),
                evidence.rating(),
                evidence.content(),
                evidence.editedAt(),
                evidence.provenance(),
                evidence.imagesComplete(),
                adminImageUrls(evidence),
                evidence.capturedAt());
    }

    private static List<String> adminImageUrls(ReviewEvidenceItem evidence) {
        return IntStream.range(0, evidence.imageUrls().size())
                .mapToObj(sortOrder -> "/api/v1/admin/review-evidence/"
                        + evidence.id() + "/images/" + sortOrder)
                .toList();
    }
}
