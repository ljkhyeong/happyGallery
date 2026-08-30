package com.personal.happygallery.adapter.in.web.product.dto;

import static com.personal.happygallery.adapter.in.web.MaskingUtil.maskName;

import com.personal.happygallery.application.qna.port.in.ProductQnaUseCase.PublicQnaListView;
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
    public static ProductQnaListItem from(PublicQnaListView qna) {
        String displayTitle = qna.secret() ? "[비밀글입니다]" : qna.title();
        return new ProductQnaListItem(
                qna.id(), displayTitle, maskName(qna.authorName()),
                qna.secret(), qna.hasReply(), qna.createdAt());
    }
}
