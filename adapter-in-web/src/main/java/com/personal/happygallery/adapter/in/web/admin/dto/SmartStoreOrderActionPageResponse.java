package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.ActionHistoryResult;
import com.personal.happygallery.application.shared.page.CursorPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record SmartStoreOrderActionPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<SmartStoreOrderActionHistoryResponse> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String nextCursor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean hasMore
) {
    public static SmartStoreOrderActionPageResponse from(CursorPage<ActionHistoryResult> page) {
        return new SmartStoreOrderActionPageResponse(
                page.content().stream().map(SmartStoreOrderActionHistoryResponse::from).toList(),
                page.nextCursor(),
                page.hasMore());
    }
}
