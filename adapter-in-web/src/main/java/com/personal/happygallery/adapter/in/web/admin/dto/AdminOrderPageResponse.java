package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.port.in.AdminOrderResponse;
import com.personal.happygallery.application.shared.page.CursorPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record AdminOrderPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<AdminOrderListItemResponse> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String nextCursor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean hasMore
) {

    public static AdminOrderPageResponse from(CursorPage<AdminOrderResponse> page) {
        return new AdminOrderPageResponse(
                page.content().stream().map(AdminOrderListItemResponse::from).toList(),
                page.nextCursor(),
                page.hasMore());
    }
}
