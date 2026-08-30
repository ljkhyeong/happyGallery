package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.qna.port.in.ProductQnaUseCase.QnaWithAuthor;
import com.personal.happygallery.application.shared.page.CursorPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record AdminQnaPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<AdminQnaResponse> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String nextCursor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        boolean hasMore
) {

    public static AdminQnaPageResponse from(CursorPage<QnaWithAuthor> page) {
        return new AdminQnaPageResponse(
                page.content().stream().map(AdminQnaResponse::from).toList(),
                page.nextCursor(),
                page.hasMore());
    }
}
