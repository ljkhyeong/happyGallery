package com.personal.happygallery.adapter.in.web.product.dto;

import static com.personal.happygallery.adapter.in.web.MaskingUtil.maskName;

import com.personal.happygallery.application.qna.port.in.ProductQnaUseCase.QnaWithAuthor;
import com.personal.happygallery.domain.qna.ProductQna;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record ProductQnaListItem(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String authorName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean secret,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean hasReply,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt
) {
    public static ProductQnaListItem from(QnaWithAuthor qa) {
        ProductQna q = qa.qna();
        String displayTitle = q.isSecret() ? "[비밀글입니다]" : q.getTitle();
        return new ProductQnaListItem(
                q.getId(), displayTitle, maskName(qa.authorName()),
                q.isSecret(), q.hasReply(), q.getCreatedAt());
    }
}
