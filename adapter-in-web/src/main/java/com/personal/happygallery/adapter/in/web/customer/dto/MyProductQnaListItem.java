package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.domain.qna.ProductQna;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record MyProductQnaListItem(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean secret,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean hasReply,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt
) {
    public static MyProductQnaListItem from(ProductQna qna) {
        return new MyProductQnaListItem(
                qna.getId(),
                qna.getTitle(),
                qna.isSecret(),
                qna.hasReply(),
                qna.getCreatedAt());
    }
}
