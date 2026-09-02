package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.ChannelOrderResult;
import com.personal.happygallery.application.shared.page.CursorPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record SmartStoreChannelOrderPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<SmartStoreChannelOrderResponse> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String nextCursor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean hasMore
) {
    public static SmartStoreChannelOrderPageResponse from(CursorPage<ChannelOrderResult> page) {
        return new SmartStoreChannelOrderPageResponse(
                page.content().stream().map(SmartStoreChannelOrderResponse::from).toList(),
                page.nextCursor(),
                page.hasMore());
    }
}
