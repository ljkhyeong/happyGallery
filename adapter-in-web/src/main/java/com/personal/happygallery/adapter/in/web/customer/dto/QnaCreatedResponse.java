package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.domain.qna.ProductQna;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record QnaCreatedResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long productId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean secret,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt
) {
    public static QnaCreatedResponse from(ProductQna q) {
        return new QnaCreatedResponse(q.getId(), q.getProductId(),
                q.getTitle(), q.isSecret(), q.getCreatedAt());
    }
}
