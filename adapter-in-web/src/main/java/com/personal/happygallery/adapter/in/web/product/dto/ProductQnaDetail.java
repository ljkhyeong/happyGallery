package com.personal.happygallery.adapter.in.web.product.dto;

import static com.personal.happygallery.adapter.in.web.MaskingUtil.maskName;

import com.personal.happygallery.application.qna.port.in.ProductQnaUseCase.QnaWithAuthor;
import com.personal.happygallery.domain.qna.ProductQna;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record ProductQnaDetail(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long productId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String replyContent,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime repliedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean secret,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String authorName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt
) {
    public static ProductQnaDetail from(QnaWithAuthor qa) {
        ProductQna q = qa.qna();
        return new ProductQnaDetail(
                q.getId(), q.getProductId(), q.getTitle(), q.getContent(),
                q.getReplyContent(), q.getRepliedAt(),
                q.isSecret(), maskName(qa.authorName()), q.getCreatedAt());
    }
}
