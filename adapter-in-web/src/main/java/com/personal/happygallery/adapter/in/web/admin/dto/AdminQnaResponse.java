package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.qna.port.in.ProductQnaUseCase.QnaWithAuthor;
import com.personal.happygallery.domain.qna.ProductQna;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record AdminQnaResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long productId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long userId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String authorName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean secret,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String replyContent,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime repliedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt
) {
    public static AdminQnaResponse from(QnaWithAuthor qa) {
        ProductQna q = qa.qna();
        return new AdminQnaResponse(
                q.getId(), q.getProductId(), q.getUserId(), qa.authorName(),
                q.getTitle(), q.getContent(), q.isSecret(),
                q.getReplyContent(), q.getRepliedAt(), q.getCreatedAt());
    }
}
