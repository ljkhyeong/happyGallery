package com.personal.happygallery.adapter.in.web.product.dto;

import com.personal.happygallery.application.qna.port.in.ProductQnaUseCase.PublicQnaListView;
import com.personal.happygallery.application.shared.page.CursorPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ProductQnaPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<ProductQnaListItem> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String nextCursor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        boolean hasMore
) {
    public static ProductQnaPageResponse from(CursorPage<PublicQnaListView> page) {
        return new ProductQnaPageResponse(
                page.content().stream().map(ProductQnaListItem::from).toList(),
                page.nextCursor(),
                page.hasMore());
    }
}
