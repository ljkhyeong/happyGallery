package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.application.qna.port.in.ProductQnaUseCase.OwnedQnaListView;
import com.personal.happygallery.application.shared.page.CursorPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record MyProductQnaPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<MyProductQnaListItem> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String nextCursor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        boolean hasMore
) {
    public static MyProductQnaPageResponse from(CursorPage<OwnedQnaListView> page) {
        return new MyProductQnaPageResponse(
                page.content().stream().map(MyProductQnaListItem::from).toList(),
                page.nextCursor(),
                page.hasMore());
    }
}
